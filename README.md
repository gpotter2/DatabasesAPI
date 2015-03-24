# DatabasesAPI
A simple api to use SQLite and MySQL in java

SQLite Use Exemple:

public void start(){
  DatabasesHandler databasesHandler;
  String path = "C:/MySuperProgramm/users.db";
  databasesHandler = new DatabasesHandler(DatabaseType.SQLITE, "table", path);
  databasesHandler.init("localhost", "database", "password", "user", "3306", "id");
  databasesHandler.addColumn("name", ObjectType.VARCHAR);
  databasesHandler.addColumn("value", ObjectType.VARCHAR);
}
public void addContent(){
  DataObject[] da = new DataObject[2];
  da[0] = new DataObject("test", "key1");
  da[1] = new DataObject("hey world !", "key2");
  databasesHandler.InsertOrUpdateValue("key1", "test", da);
}
