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
package leap.orm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import leap.lang.enums.Bool;
import leap.lang.jdbc.JdbcTypes;

@Target({ElementType.FIELD,ElementType.METHOD,ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    
    int ORDER_MIDDLE = 50;
    int ORDER_LAST   = 1000;
	
	/**
	 * (Optional) The name of the mapping column.
	 */
	String value() default "";
	
	/**
	 * (Optional) The name of the mapping column.
	 */
	String name() default "";
	
	/**
	 * (Optional) The jdbc type of the mapping column.
	 * 
	 * @see JdbcTypes
	 */
	ColumnType type() default ColumnType.AUTO;
	
	/**
	 * (Optional) The jdbc type name of the mapping column.
	 */
	String typeName() default "";
	
	/**
	 * (Optional) The column length. (Applies only if a string-valued column is used.)
	 */
	int length() default Integer.MIN_VALUE;
	
	/**
	 * (Optional) The precision for a decimal column. (Applies only if a decimal column is used.)
	 */
	int precision() default Integer.MIN_VALUE;
	
	/**
	 * (Optional) The scale for a decimal column. (Applies only if a decimal column is used.)
	 */
	int scale() default Integer.MIN_VALUE;
	
	/**
	 * (Optional) Whether the column is nullable.
	 */
	Bool nullable() default Bool.NONE;
	
	/**
	 * (Optional) Whether the column is a unique key.
	 */
	Bool unique() default Bool.NONE;

	/**
	 * (Optional) Whether the column is included in SQL INSERT statements generated by the persistence provider.
	 */
	Bool insert() default Bool.NONE;
	
	/**
	 * (Optional) Whether the column is included in SQL UPDATE statements generated by the persistence provider.
	 */
	Bool update() default Bool.NONE;
	
	/**
	 * (Optional) The default value of the column.
	 */
	String defaultValue() default "";
	
	/**
	 * The sort order of column.
	 */
	int order() default Integer.MIN_VALUE;
}