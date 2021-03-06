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
package leap.orm.command;

import leap.core.jdbc.PreparedStatementHandler;
import leap.db.Db;
import leap.lang.Args;
import leap.lang.Arrays2;
import leap.lang.Strings;
import leap.lang.expression.Expression;
import leap.lang.params.Params;
import leap.orm.OrmContext;
import leap.orm.dao.Dao;
import leap.orm.generator.ValueGeneratorContext;
import leap.orm.interceptor.EntityExecutionContext;
import leap.orm.listener.PreCreateEntity;
import leap.orm.mapping.EntityMapping;
import leap.orm.mapping.FieldMapping;
import leap.orm.sql.SqlCommand;
import leap.orm.sql.SqlFactory;
import leap.orm.value.Entity;

import java.util.Map;

public class DefaultInsertCommand extends AbstractEntityDaoCommand implements InsertCommand,ValueGeneratorContext,EntityExecutionContext {
	
    protected final SqlFactory sf;
    protected final Entity     entity;
	
	protected SqlCommand   command;
	protected FieldMapping fm;
	protected Object	   generatedId;
	protected Params       parameters;

	public DefaultInsertCommand(Dao dao,EntityMapping em) {
		this(dao, em, null);
	}
	
	public DefaultInsertCommand(Dao dao,EntityMapping em, SqlCommand command) {
	    super(dao,em);
	    this.sf		 = dao.getOrmContext().getSqlFactory();
	    this.entity  = new Entity(em.getEntityName());
	    this.command = command;
    }
	
	@Override
    public EntityMapping getEntityMapping() {
	    return em;
    }
	
	@Override
    public Object getGeneratedId() {
	    return generatedId;
    }
	
	@Override
    public boolean isReturnGeneratedId() {
	    return true;
    }

	@Override
    public void setGeneratedId(Object id) {
		this.generatedId = id;
		
		if(null != parameters){
			parameters.set(em.getKeyFieldNames()[0], id);
		}
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public InsertCommand setAll(Object bean) {
		Args.notNull(bean,"bean");
		
		if(bean instanceof PreCreateEntity) {
		    ((PreCreateEntity) bean).preCreate();
		}
		
		if(bean instanceof Map){
			return setAll((Map)bean);
		}else{
			this.parameters = context.getParameterStrategy().createParams(bean);
			this.entity.putAll(parameters.map());
		    return this;
		}
    }
	
	@Override
    public InsertCommand setAll(Map<String, Object> fields) {
		Args.notNull(fields,"properties");
		this.parameters = context.getParameterStrategy().createParams(fields);
		entity.putAll(fields);
		return this;
    }

	@Override
    public InsertCommand set(String name, Object value) {
		Args.notEmpty(name,"name");
		this.entity.put(name, value);
	    return this;
    }
	
	@Override
    public OrmContext getOrmContext() {
	    return dao.getOrmContext();
    }

	@Override
    public FieldMapping getFieldMapping() {
	    return fm;
    }

	@Override
    public Params getParameters() {
	    return entity;
    }

	@Override
    public int execute() {
		prepare();
		
		PreparedStatementHandler<Db> handler = null;
		
		if(null != em.getInsertInterceptor()){
			handler = em.getInsertInterceptor().getPreparedStatementHandler(this);
		}
		
		SqlCommand command = this.command;
		
		if(null == command){
            String[] fields = entity.keySet().toArray(Arrays2.EMPTY_STRING_ARRAY);

			command = sf.createInsertCommand(context, em, fields);
		}
		
		if(null != handler){
			return command.executeUpdate(this,entity,handler);	
		}else{
			return command.executeUpdate(this,entity);
		}
    }
	
	protected void prepare(){
		for(FieldMapping fm : em.getFieldMappings()){
			this.fm = fm;
			
			if(!Strings.isEmpty(fm.getSequenceName())){
				//the insertion sql must use $fieldName$
				entity.set(fm.getFieldName(), db.getDialect().getNextSequenceValueSqlString(fm.getSequenceName()));
			}else{
				Expression expression = fm.getInsertValue();
				if(null != expression){
					if(null == entity.get(fm.getFieldName())) {
						Object value = expression.getValue(this, entity);	
						
						if(fm.isPrimaryKey()){
							generatedId = value;
						}
						
						setGeneratedValue(fm, value);
					}
				}else if(null == entity.get(fm.getFieldName())){
					expression = fm.getDefaultValue();
					if(null != expression){
						setGeneratedValue(fm,expression.getValue(this,entity));
					}
				}
			}
		}
		this.fm = null;
	}
	
	protected void setGeneratedValue(FieldMapping fm,Object value){
		entity.put(fm.getFieldName(), value);
		
		if(null != parameters){
			parameters.set(fm.getFieldName(), value);
		}
	}
}