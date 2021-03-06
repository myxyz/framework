/*
 * Copyright 2013 the original author or authors.
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
package leap.orm.generator;

import java.sql.Types;
import java.util.function.Consumer;

import leap.core.annotation.Inject;
import leap.core.jdbc.PreparedStatementHandler;
import leap.core.validation.annotations.NotNull;
import leap.db.Db;
import leap.lang.Strings;
import leap.lang.logging.Log;
import leap.lang.logging.LogFactory;
import leap.orm.annotation.Sequence;
import leap.orm.interceptor.EntityExecutionContext;
import leap.orm.interceptor.EntityExecutionInterceptor;
import leap.orm.mapping.EntityMappingBuilder;
import leap.orm.mapping.FieldMappingBuilder;
import leap.orm.mapping.SequenceMappingBuilder;
import leap.orm.metadata.MetadataContext;

public class AutoIdGenerator implements IdGenerator {
	
	private static final Log log = LogFactory.get(AutoIdGenerator.class);
	
	protected @NotNull ValueGenerator uuidGenerator;
	
	protected int uuidLength = 38;
	
	@Inject(name="uuid")
	public void setUuidGenerator(ValueGenerator uuidGenerator) {
		this.uuidGenerator = uuidGenerator;
	}
	
	public void setUuidLength(int uuidLength) {
		this.uuidLength = uuidLength;
	}

	@Override
    public void mapping(MetadataContext context, EntityMappingBuilder emb, FieldMappingBuilder fmb) {
		Db db = context.getDb();
		
		//smallint, integer or big integer type for sequence , identity or table generator 
		if(isIntegerType(fmb)){
			
			if(db.getDialect().supportsAutoIncrement()){
				mappingAutoIncrement(context, emb, fmb);
			}else if(db.getDialect().supportsSequence()){
				mappingSequence(context, emb, fmb);
			}else{
				//TODO : table sequence generator
				throw new IllegalStateException("db '" + db.getDescription() + "' not supports identity and sequence, can not use autoid");
			}
			
			return ;
		}
		
		//varchar type for guid generator
		if(isGuidType(fmb)){
			mappingUUID(context, emb, fmb);
		}
    }
	
	protected void mappingAutoIncrement(MetadataContext context, EntityMappingBuilder emb, FieldMappingBuilder fmb){
		fmb.getColumn().setAutoIncrement(true);
		fmb.setInsert(false);  //remove auto increment from insert columns
		
		emb.setInsertInterceptor(new EntityExecutionInterceptor() {
			@Override
			public PreparedStatementHandler<Db> getPreparedStatementHandler(final EntityExecutionContext context) {
				if(!context.isReturnGeneratedId()){
					return null;
				}
				
				return context.getOrmContext().getDb().getDialect().getAutoIncrementIdHandler(new Consumer<Object>() {
					@Override
					public void accept(Object input) {
						context.setGeneratedId(input);
					}
				});
			}
		});
	}
	
	protected void mappingSequence(MetadataContext context, EntityMappingBuilder emb,final FieldMappingBuilder fmb){
		SequenceMappingBuilder seq = new SequenceMappingBuilder();
		
		setSequenceProperties(context, emb, fmb, seq);
		
		fmb.setSequenceName(seq.getName());
		
		if(null == context.getMetadata().tryGetSequenceMapping(seq.getName())){
			context.getMetadata().addSequenceMapping(seq.build());
		}else{
			log.info("Sequence '{}' aleady exists, skip adding it into the metadata",seq.getName());
		}
		
		emb.setInsertInterceptor(new EntityExecutionInterceptor() {
			@Override
			public PreparedStatementHandler<Db> getPreparedStatementHandler(final EntityExecutionContext context) {
				if(!context.isReturnGeneratedId()){
					return null;
				}
				return context.getOrmContext().getDb().getDialect()
							  .getInsertedSequenceValueHandler(fmb.getSequenceName(),
								   new Consumer<Object>() {
									@Override
			                        public void accept(Object input) {
										context.setGeneratedId(input);
			                        }
								});
			}
		});
	}
	
	protected void mappingUUID(MetadataContext context, EntityMappingBuilder emb, FieldMappingBuilder fmb){
		fmb.setValueGenerator(uuidGenerator);
		int length = fmb.getColumn().getLength() == null?uuidLength:fmb.getColumn().getLength();
		if(length < uuidLength){
			String warnInfo = "the column of field `"+fmb.getFieldName()+"` in "+emb.getEntityName()+" is an uuid field, it's length must longer than " + uuidLength;
			log.warn(warnInfo);
		}
		fmb.getColumn().setLength(length);
	}
	
	protected void setSequenceProperties(MetadataContext context,
										 EntityMappingBuilder emb,FieldMappingBuilder fmb,SequenceMappingBuilder seq){
	
		Sequence a = fmb.getBeanProperty() != null ? fmb.getBeanProperty().getAnnotation(Sequence.class) : null;
		if(null != a){
			seq.setName(Strings.firstNotEmpty(a.name(),a.value()));
			
			if(a.start() != Long.MAX_VALUE){
				seq.setStart(a.start());
			}
			
			if(a.increment() != Integer.MIN_VALUE){
				seq.setIncrement(a.increment());
			}
			
			if(a.cache() != Integer.MIN_VALUE){
				seq.setCache(a.cache());
			}
		}
		
		seq.setSchema(emb.getTableSchema());
		
		if(Strings.isEmpty(seq.getName())){
			seq.setName(context.getNamingStrategy().generateSequenceName(emb.getTableName(), fmb.getColumn().getName()));
		}
	}
	
	protected boolean isIntegerType(FieldMappingBuilder fmb) {
		int typeCode = fmb.getColumn().getTypeCode();
		
		if(Types.SMALLINT == typeCode || Types.INTEGER == typeCode || Types.BIGINT == typeCode){
			return true;
		}
		
		return false;
	}
	
	protected boolean isGuidType(FieldMappingBuilder fmb){
		if(null != fmb.getBeanProperty()){
			return fmb.getBeanProperty().getType().equals(String.class);
		}
		
		if(fmb.getColumn().getTypeCode() == Types.VARCHAR){
			return true;
		}
		
		return false;
	}
}