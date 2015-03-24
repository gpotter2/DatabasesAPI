package test.test.test.databases;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.AccessDeniedException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import test.test.test.databases.DatabasesUtil.DataObject;
import test.test.test.databases.DatabasesUtil.MySQL;
import test.test.test.databases.DatabasesUtil.SQLite;

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
			if(createTable()){
				if(!sqllite_useable()){
					System.err.println("SQLite is not installed !");
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
			new NullPointerException("The DatabaseType cannot be null !").printStackTrace();
			return false;
		}
	}
	
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
	 */
	
	public List<Object> getValues(String data, String column_key, String key){
		if(init){
			if(database_used.equals(DatabaseType.MYSQL)){
				return getValueMySQL(data, column_key, key);
			} else if(database_used.equals(DatabaseType.SQLITE)){
				return getValueSQLite(data, column_key, key);
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
	
	private String getValueCommandStringSet(ArrayList<String> keys, ArrayList<String> values){
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
        for(String value : values){
        	if(main.endsWith("'")){
        		main = main + ", '" + value + "'";
        	} else {
        		main = main + "'" + value + "'";
        	}
        }
        main = main + ");";
        return main;
	}
	
	private List<String> getValueCommandStringUpdate(String column_refer, String key_not_changed, ArrayList<String> keys, ArrayList<String> values){
		List<String> commands = new LinkedList<String>();
		for(Object actual_primary_key : getValues(primary_key, column_refer, key_not_changed)){
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
	        for(String value : values){
	        	main = main + ", '" + value + "'";	
	        }
	        main = main + ")";
	        commands.add(main);
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
	 * 
	 */
	
	public void InsertValueForce(DataObject[] objects){
		if(init){
			ArrayList<String> keys = new ArrayList<String>(objects.length + 1);
			ArrayList<String> values = new ArrayList<String>(objects.length + 1);
			for(DataObject d : objects){
				keys.add(d.getKey());
				values.add(d.getData());
			}
			if(database_used.equals(DatabaseType.MYSQL)){
				MySQL db = new MySQL(HOST, USER, PASS, DATABASE, PORT);
				try {
					db.open(false);
					if(!db.checkTable(table)){
						createTable();
					}
				} catch (SQLException | ConnectException e) {
					System.err.println("Error while checking and creating databse !");
				}
	            try {
	            	String command = getValueCommandStringSet(keys, values);
	                db.update(command);
	            } catch (SQLException ex) {
	            	ex.printStackTrace();
	            	System.err.println("Error while setting a value in MySQL !");
	            } finally {
	                db.close();
	            }
			} else if(database_used.equals(DatabaseType.SQLITE)){
				SQLite db = new SQLite();
				try {
					System.out.println("DEBUG='" + table + "'");
					if(!db.checkTable(table)){
						createTable();
					}
				} catch (SQLException e) {
					System.err.println("Error while checking and creating databse !");
				}
                try {
                	String command = getValueCommandStringSet(keys, values);
                    db.open(path, false);
                    db.update(command);
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
	 * It will replace all the values if:<br/>
	 * "WHERE [column_refer]=[key_not_changed]<br/>
	 * 
	 * @param column_refer The column wher the key_not_changed will be tested !
	 * @param key_not_changed The key to identify the line that must be edited.
	 * @param objects A list of DataObject to set on a line
	 * 
	 */
	
	public void InsertOrUpdateValue(String column_refer, String key_not_changed, DataObject[] objects){
		if(init){
			ArrayList<String> keys = new ArrayList<String>(objects.length + 1);
			ArrayList<String> values = new ArrayList<String>(objects.length + 1);
			for(DataObject d : objects){
				keys.add(d.getKey());
				values.add(d.getData());
			}
			if(database_used.equals(DatabaseType.MYSQL)){
				MySQL db = new MySQL(HOST, USER, PASS, DATABASE, PORT);
	            try {
	            	db.open(false);
	            	List<String> all_commands = getValueCommandStringUpdate(column_refer, key_not_changed, keys, values);
	            	for(String command : all_commands){
	            		db.update(command);
	            	}
	            } catch (SQLException | ConnectException ex) {
	            	ex.printStackTrace();
	            	System.err.println("Error while setting a value in MySQL !");
	            } finally {
	                db.close();
	            }
			} else if(database_used.equals(DatabaseType.SQLITE)){
				SQLite db = new SQLite();
                try {
                	List<String> all_commands = getValueCommandStringUpdate(column_refer, key_not_changed, keys, values);
                	db.open(path, false);
                	for(String command : all_commands){
                		System.out.println(command);
	            		db.update(command);
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
	
	public void existInTable(String column, String key){
		if(init){
			String command = "IF EXISTS (SELECT * FROM TABLE WHERE " + column + "='" + key + "')";
			if(database_used.equals(DatabaseType.MYSQL)){
				MySQL db = new MySQL(HOST, USER, PASS, DATABASE, PORT);
	            try {
	                db.open(false);
	                ResultSet result = db.query(command);
                    if (result.next()) {
                    	result.getObject(1);
                    }
	            } catch (SQLException | ConnectException ex) {
	            	ex.printStackTrace();
	            	System.err.println("Error while testing a value in MySQL !");
	            } finally {
	                db.close();
	            }
			} else if(database_used.equals(DatabaseType.SQLITE)){
				SQLite db = new SQLite();
                try {
                    db.open(path, false);
                    ResultSet result = db.query(command);
                    if (result.next()) {
                    	result.getObject(1);
                    }
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
	
	/**
	 * 
	 * Create the table if does not exist
	 * 
	 * @return True if the table was successfuly created or there was no table to create
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
	 * 
	 */
	
	public void clearTable(){
		if(init){
			String command_clear = "DELETE FROM " + table + ";";
			String command_reset = "ALTER TABLE " + table + " AUTO_INCREMENT = 1" + ";";
			if(database_used.equals(DatabaseType.MYSQL)){
				MySQL db = new MySQL(HOST, USER, PASS, DATABASE, PORT);
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
				SQLite db = new SQLite();
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

	
	private boolean mysql_installed(){
		try {
			Class.forName("com.mysql.jdbc.Driver");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
	
	private boolean mysql_useable(){
		MySQL db = null;
		try {
			db = new MySQL(HOST, USER, PASS, DATABASE, PORT);
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
	
	private boolean sqllite_useable(){
		SQLite db = null;
		try {
			Class.forName("org.sqlite.JDBC");
			db = new SQLite();
			db.open(path, false);
		    if(db.checkTable(table)){
		    	db.close();
		    	return true;
		    }
		    System.err.println("The table couldn't be created !");
		} catch (ClassNotFoundException e) {
			System.err.println("SQLite is not installed !");
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
	
	private boolean createTableSQLite() {
		SQLite db = new SQLite();
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
            String command = "CREATE TABLE IF NOT EXISTS " + table + " ( " + primary_key + " INTEGER NOT NULL PRIMARY KEY);";
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
	
	private List<Object> getValueSQLite(String data, String column_key, String key) {
		SQLite db = new SQLite();
		List<Object> ret = new LinkedList<Object>();
        try {
            db.open(path, false);
            String command = "SELECT * FROM `" + table + "` WHERE " + column_key + "='" + key + "';";
            ResultSet result = db.query(command);
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
	
	private boolean columnExistSQLite(String column){
		SQLite db = new SQLite();
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
	
	private void addColumnSQLite(String column, ObjectType type) {
		SQLite db = new SQLite();
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
	
	private boolean createTableMySQL(){
		MySQL db = new MySQL(HOST, USER, PASS, DATABASE, PORT);
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
	
	private List<Object> getValueMySQL(String data, String column_key, String key) {
			MySQL db = new MySQL(HOST, USER, PASS, DATABASE, PORT);
			List<Object> ret = new LinkedList<Object>();
	        try {
	            db.open(false);
	            String command = "SELECT * FROM `" + table + "` WHERE " + column_key + "='" + key + "';";
	            ResultSet result = db.query(command);
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
	
	private boolean columnExistMySQL(String column){
		MySQL db = new MySQL(HOST, USER, PASS, DATABASE, PORT);
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
	
	private void addColumnMySQL(String column, ObjectType type) {
		MySQL db = new MySQL(HOST, USER, PASS, DATABASE, PORT);
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
