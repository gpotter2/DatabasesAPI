public class Main {

public void start(){
  DatabasesHandler databasesHandler;
  String path = "C:/MySuperProgramm/users.db";
  databasesHandler = new DatabasesHandler(DatabaseType.SQLITE, "table", path);
  databasesHandler.init("localhost", "database", "password", "user", "3306", "id");
  databasesHandler.addColumn("name", ObjectType.VARCHAR);
  databasesHandler.addColumn("value", ObjectType.VARCHAR);
}
public void addContent(){//Will insert or replace a row with the object da[0] and da[1]
  DataObject[] da = new DataObject[2];
  da[0] = new DataObject("test", "key1");
  da[1] = new DataObject("hey world !", "key2");
  databasesHandler.InsertOrUpdateValue("key1", "test", da);
}
public void getContent(){//Will get the 'key2' where key1='test'
  List<Object> result = databasesHandler.getValues("key2", "key1", "test");
  System.out.println(result);
}
}