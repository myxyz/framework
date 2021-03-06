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
package leap.db.mock;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import leap.lang.exception.NestedSQLException;

public class MockDriver implements Driver {
	
	public static final String CLASS_NAME = MockDriver.class.getName();
	public static final String JDBC_URL   = "jdbc:leapcp:test";
	
	static {
		try {
	        DriverManager.registerDriver(new MockDriver());
        } catch (SQLException e) {
        	throw new NestedSQLException(e);
        }
	}
	
	private final MockDataSource dataSource;
	
	public MockDriver() {
		dataSource = new MockDataSource();
	}
	
	public MockDataSource getDataSource() {
		return dataSource;
	}

	@Override
	public Connection connect(String url, Properties info) throws SQLException {
		return dataSource.getConnection();
	}

	@Override
	public boolean acceptsURL(String url) throws SQLException {
		return JDBC_URL.equals(url);
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		return null;
	}

	@Override
	public int getMajorVersion() {
		return 0;
	}

	@Override
	public int getMinorVersion() {
		return 0;
	}

	@Override
	public boolean jdbcCompliant() {
		return true;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return null;
	}
}