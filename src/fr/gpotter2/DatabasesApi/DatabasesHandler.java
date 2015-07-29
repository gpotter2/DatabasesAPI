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

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.AccessDeniedException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import DatabasesUtil.DataObject;
import DatabasesUtil.UtilMySQL;
import DatabasesUtil.UtilSQLite;

/**
 * 
 * The DatabasesHandler main class:<br/>
 * The simple api to use MySQL and SQLite !!
 * 
 * @author gpotter2
 * @version 1.3
 *
 */

public class DatabasesHandler {
	
	private String path;
	private String table;
	private DatabaseType database_used;
	
	private String primary_key;
	
	/* MYSQL */
    private String HOST = "";
    private String USER = "";
    private String PASS = "";
    private String DATABASE = "";
    private String PORT = "";
    /*       */
	
    private boolean init = false;
    private int constructor_used;
    
    public enum DatabaseType {
    	MYSQL,
    	SQLITE;
    }
    /**
     * 
     * Create a new DatabaseHandler instance for a MySQL use.<br/>
     * 
     * @see DatabasesHandler(DatabaseType database_used, String table_name, String path)
     * 
     * @param database_used The database name
     * @param table_name The table name
     */
    public DatabasesHandler(DatabaseType database_used, String table_name){
    	this.database_used = database_used;
		this.table = table_name;
		this.constructor_used = 0;
    }
    
    /**
     * 
     * Create a new DatabaseHandler instance for a SQLite use.<br/>
     * 
     * @see DatabasesHandler(DatabaseType database_used, String table_name)
     * 
     * @param database_used The database name
     * @param table_name The table name
     * @param path The path of the db file to use. May be automatically created.
     * 
     */
    
	public DatabasesHandler(DatabaseType database_used, String table_name, String path){
		this.database_used = database_used;
		this.table = table_name;
		this.path = path;
		this.constructor_used = 1;
	}
	
	/**
	 * 
	 * Init the database, and create the table if it doesn't exist
	 * 
	 * @param host The host to connect. Null for SQLite
	 * @param user The username to connect to the database. Null for SQLite
	 * @param pass The password to connect to the database. Null for SQLite
	 * @param database The database name
	 * @param port The port of the database. Null for SQLite
	 * @param primary_key_name The name of the primary key (this key musn't never be used somewhere else)
	 * @return True if the database was initied, of false if the connection was impossible
	 * @author gpotter2
	 * 
	 */
	
	public boolean init(String host, String user, String pass, String database, String port, String primary_key_name){
		long start_time = System.currentTimeMillis();
		this.HOST = host;
		this.USER = user;
		this.PASS = pass;
		this.DATABASE = database;
		this.PORT = port;
		this.primary_key = primary_key_name;
		System.out.println("Loading database...");
		if(database_used.equals(DatabaseType.MYSQL)){
			if(constructor_used != 0){
				new IllegalAccessError("You used the wrong constructor to create this database !").printStackTrace();
				return false;
			}
			System.out.println("Checking connexion...");
			if(mysql_installed()){
				if(createTable()){
					if(!mysql_useable()){
						System.err.println("Cannot connect to the database !");
						return false;
					} else {
						init = true;
						long done_in = System.currentTimeMillis() - start_time;
						System.out.println("Database loaded ! Done in " + done_in + "ms !");
						return true;
					}
				} else {
					System.err.println("Cannot check for a table !");
					return false;
				}
			} else {
				System.err.println("MySQL is not installed !");
				return false;
			}
		} else if(database_used.equals(DatabaseType.SQLITE)){
			System.out.println("Checking file...");
			if(constructor_used == 1){
				if(path == null){
					new NullPointerException("The path cannot be null !").printStackTrace();
					return false;
				}
			} else {
				new IllegalAccessError("You used the wrong constructor to create this database !").printStackTrace();
				return false;
			}
			System.out.println("Checking connexion...");
			if(sqllite_installed()){
				if(createTable()){
					if(!sqllite_useable()){
						System.err.println("SQLite is not useable !");
						return false;
					} else {
						init = true;
						long done_in = System.currentTimeMillis() - start_time;
						System.out.println("Database loaded ! Done in " + done_in + "ms !");
						return true;
					}
				} else {
					System.err.println("Cannot check for a table !");
					return false;
				}
			} else {
				System.err.println("SQLite is not installed !");
				return false;
			}
		} else {
			new NullPointerException("The DatabaseType cannot be null !").printStackTrace();
			return false;
		}
	}
	
	/**
	 * 
	 * An util class used to create a condition
	 * 
	 * @author gpotter2
	 *
	 */
	public static class Condition {
		String key;
		String column_key;
		
		/**
		 * Used with:<br/>
		 * WHERE [column_key]=[key]
		 * 
		 * @author gpotter2
		 *
		 */
		
		public Condition(String column_key, String key){
			this.key = key;
			this.column_key = column_key;
		}
	}
	
	/**
	 * The different types of objects
	 * @author gpotter2
	 *
	 */
	public enum ObjectType {
		/**
		 * Represent a normal Integer:<br/>
		 * From -2,147,483,648 to +2,147,483,647
		 */
		INTEGER,
		/**
		 * Represent a big Integer:<br/>
		 * From -9,223,372,036,854,775,808 to +9,223,372,036,854,775,807
		 */
		BIGINT,
		/**
		 * Represent a single character
		 */
		CHARACTER,
		/**
		 * Represent a String:<br/>
		 * Max size: 8000 characters
		 */
		VARCHAR,
		/**
		 * Represent a boolean
		 */
		BOOLEAN,
		/**
		 * Represent a float
		 */
		FLOAT,
		/**
		 * Represent a decimal number
		 */
		DECIMAL,
		/**
		 * Represent an array
		 */
		ARRAY,
		/**
		 * Represent a date with years, month and days values
		 */
		DATE,
		/**
		 * Represent a time with hour, minute, and second values
		 */
		TIME,
		/**
		 * Represent any kind of data
		 */
		NONE,
		/**
		 * Represent a date and a time with year, month, day, hour, minute, and second values
		 */
		TIMESTAMP;
	}
	
	/**
	 * 
	 * Get the data.<br/>
	 * Use to detect:<br/>
	 * "WHERE [column_key]=[key]"
	 * 
	 * @param data The data to get
	 * @param column_key The column name were the key will be used.
	 * @param key The key in the column
	 * @return The object or null if it doesn`t exist
	 * @author gpotter2
	 */
	
	public List<Object> getValues(String data, Condition... conditions){
		if(init){
			if(database_used.equals(DatabaseType.MYSQL)){
				return getValueMySQL(data, conditions);
			} else if(database_used.equals(DatabaseType.SQLITE)){
				return getValueSQLite(data, conditions);
			}
		} else {
			new IllegalStateException("The DatabaseHandler wasn`t init !").printStackTrace();
			return null;
		}
		return null;
	}
	
	/**
	 * 
	 * Add a column in the database
	 * 
	 * @param column The column name
	 * @param type The ObjectType used
	 * @param primary If the PRIMARY KEY is set for this key
	 * @author gpotter2
	 * 
	 */
	
	public void addColumn(String column, ObjectType type){
		if(init){
			if(database_used.equals(DatabaseType.MYSQL)){
				addColumnMySQL(column, type);
			} else if(database_used.equals(DatabaseType.SQLITE)){
				addColumnSQLite(column, type);
			}
		} else {
			new IllegalStateException("The DatabaseHandler wasn`t init !").printStackTrace();
			return;
		}
		return;
	}
	
	private String getValueCommandStringSet(ArrayList<String> keys, ArrayList<Object> values){
		String main = "";
		if(database_used.equals(DatabaseType.MYSQL)){
			main = "REPLACE INTO `" + table + "` (";
		} else if(database_used.equals(DatabaseType.SQLITE)) {
			main = "INSERT OR REPLACE INTO `" + table + "` (";
		}
		for(String key : keys){
        	if(main.endsWith("`")){
        		main = main + ", `" + key + "`";
        	} else {
        		main = main + "`" + key + "`";
        	}
        }
        main = main + ") VALUES (";
        for(int i = 0; i < values.size(); i++){
        	if(main.endsWith("?")){
        		main = main + ", ?";
        	} else {
        		main = main + "?";
        	}
        }
        main = main + ");";
        return main;
	}
	
	private List<String> getValueCommandStringUpdate(ArrayList<String> keys, ArrayList<Object> values, Condition... condition){
		List<String> commands = new LinkedList<String>();
		List<Object> list_o_get = getValues(primary_key, condition);
		if(list_o_get.size() == 0){
			commands.add(getValueCommandStringSet(keys, values));
			return commands;
		} else {
			for(Object actual_primary_key : list_o_get){
				String main = "";
				if(database_used.equals(DatabaseType.MYSQL)){
					main = main + "REPLACE INTO `" + table + "` (";
				} else if(database_used.equals(DatabaseType.SQLITE)) {
					main = main + "INSERT OR REPLACE INTO `" + table + "` (";
				}
				main = main + "`" + primary_key + "`";
		        for(String key : keys){
		        	main = main + ", `" + key + "`";
		        }
		        main = main + ") VALUES (" + actual_primary_key.toString();
		        for(int i = 0; i < values.size(); i++){
		        	main = main + ", ?";	
		        }
		        main = main + ")";
		        commands.add(main);
			}
		}
        return commands;
	}
	
	/**
	 * 
	 * Force insert a value in the database.<br/>
	 * Will not replace any old value<br/>
	 * 
	 * @param objects A list of DataObject to set on a line
	 * @deprecated
	 * @author gpotter2
	 * 
	 */
	
	public void InsertValueForce(List<DataObject> objects){
		DataObject[] array = objects.toArray(new DataObject[objects.size()]);
		InsertValueForce(array);
	}
	
	/**
	 * 
	 * Force insert a value in the database.<br/>
	 * Will not replace any old value<br/>
	 * 
	 * @param objects An array of DataObject to set on a line
	 * @deprecated
	 * @author gpotter2
	 * 
	 */
	
	@SuppressWarnings("unchecked")
	public void InsertValueForce(DataObject[] objects){
		if(init){
			ArrayList<String> keys = new ArrayList<String>(objects.length + 1);
			ArrayList<Object> values = new ArrayList<Object>(objects.length + 1);
			for(DataObject d : objects){
				keys.add(d.getKey());
				values.add(d.getData());
			}
			if(database_used.equals(DatabaseType.MYSQL)){
				UtilMySQL db = new UtilMySQL(HOST, USER, PASS, DATABASE, PORT);
				try {
					db.open(false);
					String command = getValueCommandStringSet(keys, values);
	                db.update(command, values);
				} catch (SQLException | ConnectException e) {
					System.err.println("Error while setting a value in MySQL !");
				} finally {
	                db.close();
	            }
			} else if(database_used.equals(DatabaseType.SQLITE)){
				UtilSQLite db = new UtilSQLite();
                try {
                	String command = getValueCommandStringSet(keys, values);
                    db.open(path, false);
                    db.update(command, values);
                } catch (SQLException ex) {
                	ex.printStackTrace();
                	System.err.println("Error while setting a value in SQLite !");
                } finally {
                    db.close();
                }
			} else {
				new NullPointerException().printStackTrace();
				return;
			}
		} else {
			new IllegalStateException("The DatabaseHandler wasn`t init !").printStackTrace();
			return;
		}
		return;
	}
	
	/**
	 * 
	 * Insert or replace a value in the database.<br/>
	 * It will replace all the values if the condition is true<br/>
	 * 
	 * @param condition The conditions where the data will be replaced
	 * @param objects A list of DataObject to set on a line
	 * @author gpotter2
	 * 
	 */
	
	public void InsertOrUpdateValue(List<DataObject> objects, Condition... conditions){
		DataObject[] array = objects.toArray(new DataObject[objects.size()]);
		InsertOrUpdateValue(array, conditions);
	}
	
	/**
	 * 
	 * Insert or replace a value in the database.<br/>
	 * It will replace all the values if the condition is true<br/>
	 * 
	 * @param condition The conditions where the data will be replaced
	 * @param objects An array of DataObject to set on a line
	 * @author gpotter2
	 * 
	 */
	
	@SuppressWarnings("unchecked")
	public void InsertOrUpdateValue(DataObject[] objects, Condition... conditions){
		if(init){
			ArrayList<String> keys = new ArrayList<String>(objects.length + 1);
			ArrayList<Object> values = new ArrayList<Object>(objects.length + 1);
			for(DataObject d : objects){
				keys.add(d.getKey());
				values.add(d.getData());
			}
			if(database_used.equals(DatabaseType.MYSQL)){
				UtilMySQL db = new UtilMySQL(HOST, USER, PASS, DATABASE, PORT);
	            try {
	            	db.open(false);
	            	List<String> all_commands = getValueCommandStringUpdate(keys, values, conditions);
	            	for(String command : all_commands){
	            		db.update(command, values);
	            	}
	            } catch (SQLException | ConnectException ex) {
	            	ex.printStackTrace();
	            	System.err.println("Error while setting a value in MySQL !");
	            } finally {
	                db.close();
	            }
			} else if(database_used.equals(DatabaseType.SQLITE)){
				UtilSQLite db = new UtilSQLite();
                try {
                	List<String> all_commands = getValueCommandStringUpdate(keys, values, conditions);
                	db.open(path, false);
                	for(String command : all_commands){
	            		db.update(command, values);
	            	}
                } catch (SQLException ex) {
                	ex.printStackTrace();
                	System.err.println("Error while setting a value in SQLite !");
                } finally {
                    db.close();
                }
			} else {
				new NullPointerException("The database type musn't be null !").printStackTrace();
				return;
			}
		} else {
			new IllegalStateException("The DatabaseHandler wasn`t init !").printStackTrace();
			return;
		}
		return;
	}
	
	/**
	 * 
	 * Return if an object exist in the table with those conditions
	 * 
	 * @param conditions The conditions to check the tablewith
	 * @return True if the object exist, otherwise false
	 * @author gpotter2
	 */
	public boolean existInTable(Condition... conditions){
		return (numberObjectsInTable(conditions) > 0);
	}
	
	/**
	 * 
	 * Return the number of objects existing in the table with those conditions
	 * 
	 * @param conditions The conditions to check the table with
	 * @return 0 if there are no objects with those conditions, otherwise the number of objects with those conditions
	 * @author gpotter2
	 */
	public int numberObjectsInTable(Condition... conditions){
		if(init){
			String command = null;
			for(Condition c : conditions){
				if(command == null){
					command = "SELECT count(*) FROM " + table + " WHERE " + c.column_key + "=? ";
				} else {
					command = command + "AND " + c.column_key + "=? ";
				}
			}
			if(database_used.equals(DatabaseType.MYSQL)){
				UtilMySQL db = new UtilMySQL(HOST, USER, PASS, DATABASE, PORT);
	            try {
	                db.open(false);
	                ResultSet result = db.query(command, conditions);
	                byte i = 0;
	    	        if (result.next()) {
	    	            i = result.getByte(1);
	    	        }
	    	        result.close();
	    	        return i;
	            } catch (SQLException | ConnectException ex) {
	            	ex.printStackTrace();
	            	System.err.println("Error while testing a value in MySQL !");
	            } finally {
	                db.close();
	            }
			} else if(database_used.equals(DatabaseType.SQLITE)){
				UtilSQLite db = new UtilSQLite();
                try {
                    db.open(path, false);
                    ResultSet result = db.query(command, conditions);
                    byte i = 0;
	    	        if (result.next()) {
	    	            i = result.getByte(1);
	    	        }
	    	        result.close();
	    	        return i;
                } catch (SQLException ex) {
                	ex.printStackTrace();
                	System.err.println("Error while testing a value in SQLite !");
                } finally {
                    db.close();
                }
			} else {
				new NullPointerException().printStackTrace();
				return 0;
			}
		} else {
			new IllegalStateException("The DatabaseHandler wasn`t init !").printStackTrace();
			return 0;
		}
		return 0;
	}
	
	/**
	 * 
	 * Create the table if does not exist
	 * 
	 * @return True if the table was successfuly created or there was no table to create
	 * @author gpotter2
	 */
	private boolean createTable(){
		if(database_used.equals(DatabaseType.MYSQL)){
			return createTableMySQL();
		} else if(database_used.equals(DatabaseType.SQLITE)){
			return createTableSQLite();
		} else {
			new NullPointerException().printStackTrace(); 
			return false;
		}
	}
	
	
	/**
	 * 
	 * Empty the table
	 * @author gpotter2
	 * 
	 */
	
	@SuppressWarnings("deprecation")
	public void clearTable(){
		if(init){
			String command_clear = "DELETE FROM " + table;
			String command_reset;
			if(database_used == DatabaseType.MYSQL){
				command_reset = "ALTER TABLE " + table + " AUTO_INCREMENT = 1";
			} else if(database_used == DatabaseType.SQLITE) {
				command_reset = "DELETE FROM sqlite_sequence WHERE name='" + table + "'";
			} else {
				return;
			}
			if(database_used.equals(DatabaseType.MYSQL)){
				UtilMySQL db = new UtilMySQL(HOST, USER, PASS, DATABASE, PORT);
				try {
	                db.open(false);
	                db.update(command_clear);
	                db.update(command_reset);
	            } catch (SQLException | ConnectException ex) {
	            	ex.printStackTrace();
	            	System.err.println("Error while testing a value in SQLite !");
	            } finally {
	                db.close();
	            }
			} else if(database_used.equals(DatabaseType.SQLITE)){
				UtilSQLite db = new UtilSQLite();
				try {
	                db.open(path, false);
	                db.update(command_clear);
	                db.update(command_reset);
	            } catch (SQLException ex) {
	            	ex.printStackTrace();
	            	System.err.println("Error while testing a value in SQLite !");
	            } finally {
	                db.close();
	            }
			} else {
				new NullPointerException().printStackTrace();
				return;
			}
		} else {
			new IllegalStateException("The DatabaseHandler wasn`t init !").printStackTrace();
			return;
		}
	}
	
	
	private String getValueCommandStringDelete(Condition... conditions){
		String result = "DELETE FROM `" + table + "` WHERE ";
		for(int i = 0; i < conditions.length; i++){			
			Condition c = conditions[i];
			if(conditions.length > (i + 1)){
				result = result + c.column_key + "=? AND ";
			} else {
				result = result + c.column_key + "=?";
			}
		}
        return result;
	}
	
	/**
	 * 
	 * Delete all the objects with the conditions
	 * 
	 * @param conditions
	 * @author gpotter2
	 */
	public void deleteObject(Condition... conditions){
		if(init){
			if(database_used.equals(DatabaseType.MYSQL)){
				UtilMySQL db = new UtilMySQL(HOST, USER, PASS, DATABASE, PORT);
				try {
					db.open(false);
					String command = getValueCommandStringDelete(conditions);
	                db.update(command, conditions);
				} catch (SQLException | ConnectException e) {
					System.err.println("Error while setting a value in MySQL !");
				} finally {
	                db.close();
	            }
			} else if(database_used.equals(DatabaseType.SQLITE)){
				UtilSQLite db = new UtilSQLite();
                try {
                	String command = getValueCommandStringDelete(conditions);
                    db.open(path, false);
                    db.update(command, conditions);
                } catch (SQLException ex) {
                	ex.printStackTrace();
                	System.err.println("Error while setting a value in SQLite !");
                } finally {
                    db.close();
                }
			} else {
				new NullPointerException().printStackTrace();
				return;
			}
		} else {
			new IllegalStateException("The DatabaseHandler wasn`t init !").printStackTrace();
			return;
		}
		return;
	}
	
	private boolean mysql_installed(){
		try {
			Class.forName("com.mysql.jdbc.Driver");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
	
	private boolean mysql_useable(){
		UtilMySQL db = null;
		try {
			db = new UtilMySQL(HOST, USER, PASS, DATABASE, PORT);
			db.open(true);
		    if(db.checkTable(table)){
		    	db.close();
		    	return true;
		    }
		    return false;
		} catch (SQLException e) {
			System.err.println("Cannot connect to the MySQL database :");
			System.err.println(e.getMessage());
		} catch (ConnectException e) {
			System.err.println("Cannot connect to the database: please check if MySQL is running and that the port and the server name are correct !");
		} finally {
			try { db.close(); } catch(Exception e){}
		}
		return false;
	}
	
	private boolean sqllite_installed(){
		try {
			Class.forName("org.sqlite.JDBC");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
	
	private boolean sqllite_useable(){
		UtilSQLite db = null;
		try {
			db = new UtilSQLite();
			db.open(path, false);
		    if(db.checkTable(table)){
		    	db.close();
		    	return true;
		    }
		    System.err.println("The table couldn't be created !");
		} catch (SQLException e) {
			System.err.println("Cannot connect to the SQLite database :");
			System.err.println(e.getMessage());
		} finally {
			try { db.close(); } catch(Exception e){}
		}
		return false;
	}
	
	/*
	 * SQLLITE SUPPORT
	 * 
	 */
	
	@SuppressWarnings("deprecation")
	private boolean createTableSQLite() {
		UtilSQLite db = new UtilSQLite();
		File f = new File(path);
        try {
        	if(!f.getParentFile().exists()){
        		new NullPointerException("The folder where the .db file should be written does not exist !").printStackTrace();
    			return false;
        	}
        } catch (Exception e){
			new NullPointerException("The folder where the .db file should be written does not exist !").printStackTrace();
			return false;
        }
        if(!f.exists()){
        	if(f.getParentFile().canWrite()){
				try {
					new File(path).createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	} else {
        		new AccessDeniedException(f.getName()).printStackTrace();
        		return false;
        	}
        }
        try {
            db.open(path, true);
            String command = "CREATE TABLE IF NOT EXISTS " + table + " ( " + primary_key + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT);";
            db.update(command);
        } catch (SQLException ex) {
        	ex.printStackTrace();
            System.err.println("Error while creating SQLite database!");
            return false;
        } finally {
            db.close();
        }
        return true;
    }
	
	private List<Object> getValueSQLite(String data, Condition... conditions) {
		UtilSQLite db = new UtilSQLite();
		List<Object> ret = new LinkedList<Object>();
        try {
            db.open(path, false);
            String command = "SELECT * FROM `" + table + "` WHERE ";
            for(int i = 0; i < conditions.length; i++){
            	Condition con = conditions[i];
            	if(i == 0) command = command + con.column_key + "=? ";
            	else command = command + "AND " + con.column_key + "=? ";
            }
            command = command + ";";
            ResultSet result = db.query(command, conditions);
            while(result.next()){
                ret.add(result.getObject(data));
            }
        } catch (SQLException e) {
        	e.printStackTrace();
            System.err.println("Error while using SQLite !");
        } finally {
            db.close();
        }
        return ret;
    }
	
	@SuppressWarnings("deprecation")
	private boolean columnExistSQLite(String column){
		UtilSQLite db = new UtilSQLite();
        try {
            db.open(path, false);
            if (db.checkTable(table)) {
            	db.query("SELECT " + column + " FROM " + table + ";");
            }
        } catch (SQLException ex) {
            return false;
        } finally {
            db.close();
        }
        return true;
	}
	
	@SuppressWarnings("deprecation")
	private void addColumnSQLite(String column, ObjectType type) {
		UtilSQLite db = new UtilSQLite();
        try {
            db.open(path, false);
            if (db.checkTable(table)) {
            	if(!columnExistSQLite(column)){
	            	String command = "ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + type.toString() + "(8000) DEFAULT NULL;";
	                db.update(command);
            	}
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            db.close();
        }
    }
	
	/*
	 * MYSQL SUPPORT
	 * 
	 */
	
	@SuppressWarnings("deprecation")
	private boolean createTableMySQL(){
		UtilMySQL db = new UtilMySQL(HOST, USER, PASS, DATABASE, PORT);
        try {
            db.open(false);
            if (db.checkConnection()) {
                if (!db.checkTable(table)) {
                	String command = "CREATE TABLE IF NOT EXISTS `" + table + "` ( `" + primary_key + "` INTEGER NOT NULL AUTO_INCREMENT, PRIMARY KEY (`"+ primary_key + "`)) ENGINE=InnoDB;";
                	db.update(command);
                }
            }
        } catch (SQLException | ConnectException ex) {
        	System.err.println("The table couldn't be created MySQL:");
        	System.err.println(ex.getMessage());
        	return false;
        } finally {
            db.close();
        }
        return true;
	}
	
	private List<Object> getValueMySQL(String data, Condition... conditions) {
			UtilMySQL db = new UtilMySQL(HOST, USER, PASS, DATABASE, PORT);
			List<Object> ret = new LinkedList<Object>();
	        try {
	            db.open(false);
	            String command = "SELECT * FROM `" + table + "` WHERE ";
	            for(int i = 0; i < conditions.length; i++){
	            	Condition con = conditions[i];
	            	if(i == 0) command = command + con.column_key + "=? ";
	            	else command = command + "AND " + con.column_key + "=? ";
	            }
	            command = command + ";";
	            ResultSet result = db.query(command, conditions);
	            while(result.next()){
	                ret.add(result.getObject(data));
	            }
	        } catch (SQLException | ConnectException e) {
	        	e.printStackTrace();
	        	System.err.println("Error while getting a value from MySQL !");
	        } finally {
	            db.close();
	        }
	        return ret;
	}
	
	@SuppressWarnings("deprecation")
	private boolean columnExistMySQL(String column){
		UtilMySQL db = new UtilMySQL(HOST, USER, PASS, DATABASE, PORT);
        try {
        	db.open(false);
            if (db.checkTable(table)) {
            	db.query("SELECT " + column + " FROM " + table);
            }
        } catch (SQLException | ConnectException ex) {
            return false;
        } finally {
        	db.close();
        }
        return true;
	}
	
	@SuppressWarnings("deprecation")
	private void addColumnMySQL(String column, ObjectType type) {
		UtilMySQL db = new UtilMySQL(HOST, USER, PASS, DATABASE, PORT);
        try {
            db.open(false);
            if (db.checkConnection()) {
                if (db.checkTable(table)) {
                	if(!columnExistMySQL(column)){
                		db.update("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + type.toString() + "(8000) DEFAULT NULL;");
                	}
                } else {
                	System.err.println("Error while using MySQL ! Couldn`t connect to the database !");
                }
            }
        } catch (SQLException | ConnectException ex) {
        	System.err.println("Error while using MySQL ! Couldn`t connect to the database !");
        } finally {
            db.close();
        }
	}
	
}
