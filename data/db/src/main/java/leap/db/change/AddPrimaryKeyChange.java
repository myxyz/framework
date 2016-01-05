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
package leap.db.change;

import leap.db.model.DbPrimaryKey;
import leap.db.model.DbTable;
import leap.lang.Args;
import leap.lang.json.JsonWriter;


public class AddPrimaryKeyChange extends PrimaryKeyChangeBase {
	
	public AddPrimaryKeyChange(DbTable table, DbPrimaryKey newPrimaryKey) {
	    super(table, null, newPrimaryKey);
	    Args.notEmpty(newPrimaryKey,"newPrimaryKey");
    }

	@Override
    public SchemaChangeType getChangeType() {
	    return SchemaChangeType.ADD;
    }

	@Override
    public void toJson(JsonWriter writer) {
		writer.startObject();
		
		writer.property("table", table.getName())
		      .property("newPrimaryKey", newPrimaryKey);
		
		writer.endObject();
    }

}