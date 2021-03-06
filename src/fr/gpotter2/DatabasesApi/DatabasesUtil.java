/*
 *  Copyright (C) 2015 Gabriel POTTER (gpotter2)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

import java.net.ConnectException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

import DatabasesHandler.Condition;

/**
 * The DatabasesHandler util class
 * 
 * @author gpotter2
 * @version 1.3
 * 
 */

public class DatabasesUtil {

	public static class DataObject {
		
		private Object data;
		private String key;
		
		public DataObject(String key, Object data){
			this.data = data;
			this.key = key;
		}
		
		public Object getData(){
			return data;
		}
		
		public String getKey(){
			return key;
		}
	}
	
	public static abstract class UtilCommon {
		
		protected Connection con = null;
		/**
		 * 
		 * @param query
		 * @return
		 * @throws SQLException
		 * @Deprecated May be used for SQL injections !!
		 */
		@Deprecated
		public ResultSet query(String query) throws SQLException {
	        return this.con.createStatement().executeQuery(query);
	    }
		
		public ResultSet query(String query, Condition... conditions) throws SQLException {
			try {
				PreparedStatement st = this.con.prepareStatement(query);
				int i = 1;
				for(Condition c : conditions){
					st.setObject(i, c.key);
					i++;
				}
				return st.executeQuery();
			} catch (SQLException e) {
				e.printStackTrace();
			}
	    	return null;
	    }
		/**
		 * 
		 * @param query
		 * @return
		 * @throws SQLException
		 * @Deprecated May be used for SQL injections !!
		 */
		@Deprecated
	    public int update(String query) throws SQLException {
	        return this.con.createStatement().executeUpdate(query);
	    }
	    
	    public int update(String query, Condition... conditions) throws SQLException {
			try {
				PreparedStatement st = this.con.prepareStatement(query);
				int i = 1;
				for(Condition c : conditions){
					st.setObject(i, c.key);
					i++;
				}
				return st.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
	    	return -1;
	    }
	    
		@SuppressWarnings("unchecked")
		public int update(String query, ArrayList<? extends Object>... objects){
	    	try {
				PreparedStatement st = this.con.prepareStatement(query);
				int i = 1;
				for(ArrayList<? extends Object> array : objects){
					for(Object o : array){
						st.setObject(i, o);
						i++;
					}
				}
				return st.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
	    	return -1;
	    }
		
		public Connection getConnection() {
	        return this.con;
	    }
		
		public abstract void close();
		public abstract boolean checkTable(String table_name) throws SQLException;
		
	}
	
	public static class UtilMySQL extends UtilCommon {

	    private String HOST = "";
	    private String USER = "";
	    private String PASS = "";
	    private String DATABASE = "";
	    private String PORT = "";

	    public UtilMySQL(String host, String user, String pass, String database, String port) {
	        HOST = host;
	        USER = user;
	        PASS = pass;
	        DATABASE = database;
	        PORT = port;
	    }

	    public Connection open(boolean print) throws SQLException, ConnectException {
	    	Properties connectionProps = new Properties();
	        connectionProps.put("user", USER);
	        connectionProps.put("password", PASS);
	        
	        if(print) System.out.println("Trying to connect with user: " + USER);
	        this.con = DriverManager.getConnection("jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE, connectionProps);
	        if(print){
		        if(this.con == null){
		        	System.err.println("Connection failed !");
		        } else {
		        	System.out.println("Connection successed !");
		        }
	        }
	        return this.con;
	    }

	    public boolean checkTable(String tablename) throws SQLException {
	        ResultSet count = query("SELECT count(*) FROM information_schema.TABLES WHERE (TABLE_SCHEMA = '" + DATABASE + "') AND (TABLE_NAME = '" + tablename + "');");
	        byte i = 0;
	        if (count.next()) {
	            i = count.getByte(1);
	        }
	        count.close();
	        return i == 1;
	    }

	    public void close() {
	        try {
	            this.con.close();
	        } catch (SQLException e) {
	        	System.err.println("Error while closing MySQL !");
	        }
	    }

	    public boolean checkConnection() throws SQLException {
	    	if(con == null) return false;
	        ResultSet count = query("SELECT count(*) FROM information_schema.SCHEMATA");
	        boolean give = count.first();
	        count.close();
	        return give;
	    }
	}
	
	public static class UtilSQLite extends UtilCommon {
	    
	    public Connection open(String path, boolean print) throws SQLException {
	    	try {
				Class.forName("org.sqlite.JDBC").newInstance();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
	        con = DriverManager.getConnection("jdbc:sqlite:" + path);
	        if(print){
		        if(con == null){
		        	System.err.println("Connection failed !");
		        } else {
		        	System.out.println("Connection successed !");
		        }
	        }
	        return con;
	    }

	    public boolean checkTable(String tablename) throws SQLException {
	    	if(con == null) return false;
	    	String command = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='" + tablename + "'";
	        ResultSet count = query(command);
	        byte i = 0;
	        if (count.next()) {
	            i = count.getByte(1);
	        }
	        count.close();
	        return (i == 1) ? true : false;
	    }
	    
	    public void close() {
	        try {
	            con.close();
	        } catch (SQLException e) {
	            System.err.println("Error while closing SQLite !");
	        }
	    }
	}
	
}
