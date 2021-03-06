/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package leap.web.api.meta;

import leap.core.annotation.Inject;
import leap.core.meta.MTypeManager;
import leap.core.web.path.PathTemplate;
import leap.lang.Strings;
import leap.lang.TypeInfo;
import leap.lang.Types;
import leap.lang.http.HTTP;
import leap.lang.logging.Log;
import leap.lang.logging.LogFactory;
import leap.lang.meta.*;
import leap.web.App;
import leap.web.action.Action;
import leap.web.action.Argument;
import leap.web.action.Argument.Location;
import leap.web.api.config.ApiConfig;
import leap.web.route.Route;

import java.lang.reflect.Type;

public class DefaultApiMetadataFactory implements ApiMetadataFactory {

	private static final Log log = LogFactory.get(DefaultApiMetadataFactory.class);
	
	protected @Inject App				     app;
	protected @Inject ApiMetadataProcessor[] processors;
	protected @Inject MTypeManager			 mtypeManager;
	
	@Override
    public ApiMetadata createMetadata(ApiConfig c) {
		ApiMetadataBuilder md = new ApiMetadataBuilder();
		
		ApiMetadataContext context = createContext(c, md);
		
		setBaseInfo(context, md);
		
		createSecurityDefs(context, md);
		
		createPaths(context, md);
		
		return processMetadata(context, md);
    }
	
    protected ApiMetadataContext createContext(ApiConfig c, ApiMetadataBuilder md) {
		final MTypeFactory tf = createMTypeFactory(c, md);
		
		return new ApiMetadataContext() {
			
			@Override
			public MTypeFactory getMTypeFactory() {
				return tf;
			}
			
			@Override
			public ApiConfig getConfig() {
				return c;
			}
		};
	}
	
	protected MTypeFactory createMTypeFactory(ApiConfig c, ApiMetadataBuilder md) {
		return mtypeManager.factory()
				
		.setComplexTypeCreatedListener((cls, ct) -> {
			if(!md.containsModel(ct.getName())) {
				md.addModel(new ApiModelBuilder(ct));
			}
		})
		
		.setComplexTypeLocalNamer((cls) -> {
			String name = cls.getSimpleName();
			
			for(String prefix : c.getRemovalModelNamePrefixes()) {
				if(Strings.startsWithIgnoreCase(name, prefix)) {
					name = Strings.removeStartIgnoreCase(name, prefix);
					break;
				}
			}
			
			return name;
		})
		
		.create();
	}

	protected void setBaseInfo(ApiMetadataContext context, ApiMetadataBuilder md) {
		ApiConfig c = context.getConfig();
		
		md.setBasePath(c.getBasePath());
		md.setName(c.getName());
		md.setTitle(c.getTitle());
		md.setVersion(c.getVersion());
		md.setSummary(c.getSummary());
		md.setDescription(c.getDescription());
		md.addProtocols(c.getProtocols());
		md.addProduces(c.getProduces());
		md.addConsumes(c.getConsumes());
	}
	
    protected void createSecurityDefs(ApiMetadataContext context, ApiMetadataBuilder md) {
        ApiConfig c = context.getConfig();
        if(c.isOAuthEnabled()) {
            OAuth2ApiSecurityDef def =
                    new OAuth2ApiSecurityDef(c.getOAuthAuthorizationUrl(),
                                             c.getOAuthTokenUrl(),
                                             c.getOAuthScopes());
            
            md.addSecurityDef(def);
        }
    }
	
	protected ApiMetadata processMetadata(ApiMetadataContext context, ApiMetadataBuilder md) {
		for(ApiMetadataProcessor p : processors) {
			p.preProcess(context, md);
		}
		
		ApiMetadata m = md.build();
		
		for(ApiMetadataProcessor p : processors) {
			p.postProcess(context, m);
		}
		
		return m;
	}
	
	protected void createPaths(ApiMetadataContext context, ApiMetadataBuilder md) {
		for(Route route : context.getConfig().getRoutes()) {
			createApiPath(context, md, route);	
		}
	}
	
	protected void createApiPath(ApiMetadataContext context, ApiMetadataBuilder md, Route route) {
		PathTemplate pt = route.getPathTemplate();

		ApiPathBuilder path = md.getPath(pt.getTemplate());
		if(null == path) {
			path = new ApiPathBuilder();
			path.setPathTemplate(pt);
			md.addPath(path);
		}

        log.debug("Path {} -> {} :", pt, route.getAction());
		createApiOperation(context, md, route, path);
	}
	
	protected void createApiOperation(ApiMetadataContext context, ApiMetadataBuilder m, Route route, ApiPathBuilder path) {
		ApiOperationBuilder op = new ApiOperationBuilder();
		
		op.setName(route.getAction().getName());

		//Set http method
		setApiMethod(context, m, route, path, op);

        log.debug(" {}", op.getMethod());
	
		//Create parameters.
		createApiParameters(context, m, route, path, op);
		
		//Create responses.
		createApiResponses(context, m, route, path, op);
		
		path.addOperation(op);
	}
	
	protected void setApiMethod(ApiMetadataContext context, ApiMetadataBuilder m, Route route, ApiPathBuilder path, ApiOperationBuilder op) {
		String method = route.getMethod();
		
		if("*".equals(method)) {
			boolean hasBodyParameter = false;
			for(Argument a : route.getAction().getArguments()) {
				if(a.getLocation() == Location.REQUEST_BODY || a.getLocation() == Location.PART_PARAM) {
					hasBodyParameter = true;
				}
			}
			
			if(hasBodyParameter) {
				op.setMethod(HTTP.Method.POST);
			}else{
				op.setMethod(HTTP.Method.GET);
			}
		}else{
			op.setMethod(HTTP.Method.valueOf(method));
		}
	}
	
	protected void createApiParameters(ApiMetadataContext context, ApiMetadataBuilder m, Route route, ApiPathBuilder path, ApiOperationBuilder op) {
		Action action = route.getAction();

        log.trace("  Parameters({})", action.getArguments().length);
		
		for(Argument a : action.getArguments()) {
            ApiParameterBuilder p = new ApiParameterBuilder();

            p.setName(a.getName());

            log.trace("   {}", a.getName(), p.getLocation());

            p.setType(createMType(context, m, a.getTypeInfo()));
            p.setLocation(getParameterLocation(context, action, a, op, p));

            if (null != a.getRequired()) {
                p.setRequired(a.getRequired());
            } else if (p.getLocation() == ApiParameter.Location.PATH) {
                p.setRequired(true);
            }

            op.addParameter(p);
        }
	}
	
	protected void createApiResponses(ApiMetadataContext context, ApiMetadataBuilder m, Route route, ApiPathBuilder path, ApiOperationBuilder op) {
		if(route.getAction().hasReturnValue()) {
			Class<?> returnType        = route.getAction().getReturnType();
			Type     genericReturnType = route.getAction().getGenericReturnType();
			
			MType type = createMType(context, m, Types.getTypeInfo(returnType, genericReturnType));
			ApiResponseBuilder resp = ApiResponseBuilder.ok();
			resp.setType(type);
			
			op.addResponse(resp);
		}else{	
			op.addResponse(ApiResponseBuilder.ok());
		}
	}
	
	protected MType createMType(ApiMetadataContext context, ApiMetadataBuilder m, TypeInfo ti) {
		//TODO: file type
		
		if (ti.isSimpleType()) {
			return context.getMTypeFactory().getMType(ti.getType(), ti.getGenericType());
		} else if (ti.isCollectionType()) {
			TypeInfo eti = ti.getElementTypeInfo();
			MType elementType = createMType(context, m, eti);
			return new MCollectionType(elementType);
		} else {
			//Complex Type
			MType mtype = context.getMTypeFactory().getMType(ti.getType());
			
			if(mtype.isTypeRef()) {
				MComplexTypeRef ref = (MComplexTypeRef)mtype;
				return new MComplexTypeRef(ref.getRefTypeName(), ref.getRefTypeQName());
			}else{
				MComplexType ct = mtype.asComplexType();
				return new MComplexTypeRef(ct.getName());
			}
		}
	}
	
	protected ApiParameter.Location getParameterLocation(ApiMetadataContext context, Action action, Argument arg, ApiOperationBuilder o, ApiParameterBuilder p) {
		Location from = arg.getLocation();
		if(null == from || from == Location.UNDEFINED) {
			
			if(p.getType().isTypeRef() || p.getType().isCollectionType()) {
				return ApiParameter.Location.BODY;
			}else{
				if(o.getMethod() == HTTP.Method.GET) {
					return ApiParameter.Location.QUERY;
				}else{
					return ApiParameter.Location.FORM;
				}
			}
		}
		
		if(from == Location.QUERY_PARAM) {
		    return ApiParameter.Location.QUERY;
		}
		
		if(from == Location.PATH_PARAM) {
			return ApiParameter.Location.PATH;
		}
		
		if(from == Location.REQUEST_BODY) {
			return ApiParameter.Location.BODY;
		}
		
		if(from == Location.REQUEST_PARAM) {
			if(o.getMethod() == HTTP.Method.GET) {
				return ApiParameter.Location.QUERY;
			}else{
				return ApiParameter.Location.FORM;
			}
		}
		
		throw new IllegalStateException("Unsupported parameter type '" + p.getType() + "' for resolving location");
	}
	
}