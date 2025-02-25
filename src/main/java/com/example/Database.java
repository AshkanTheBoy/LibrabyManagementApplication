import java.util.Set;
import java.util.HashSet;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;

public final class Database{
    private static Database INSTANCE;//single instance - singleton
    private final Set<String> tables;//i thought that using set's is more efficient, than lists
    private final Connection connection;//single connection, because we lose data, if multiple
    private int tableIndex = 0;//incrementing index for naming tables
    private String currentTable;//currently selected table field

    private Database() throws SQLException {
        INSTANCE = this;
        tables = new HashSet<>();
        currentTable = "";
        String url = "jdbc:sqlite::memory:";
        connection = DriverManager.getConnection(url);
    }

    public static Database getDatabase() {
        if (INSTANCE==null){
            try {
                INSTANCE = new Database();
                return INSTANCE;
            } catch (SQLException e){
                System.out.println(e.getMessage());
            }
        }
        return null;
    }

    //if set contains the input as table name - select it
    //might've been better to throw an exception instead of just printing a message
    public void setCurrentTable(String tableName){
        if (tables.contains(tableName)){
            currentTable = tableName;
        } else {
            System.out.println("No such table available");
        }
    }

    public void addTable(){
        //DON'T ALLOW USER TO INPUT TABLE NAMES;)
        //just add the incrementing index and set it as our table name
        //might cause trouble for long usage, since the index does not reset when deleting tables
        String tableName = "Books_"+tableIndex++;
        tables.add(tableName);//add to the set
        try(Statement st = connection.createStatement()){
            st.execute(getCreateTableQuery(tableName));//get the query and execute it
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public void deleteSelectedTable(){
        if (tables.contains(currentTable)){//maybe an unneccessary check, but prevents from deleting "" table name
            try (Statement statement = connection.createStatement()) {
                statement.execute(getDeleteTableQuery());//get the query and execute it
                tables.remove(currentTable);//remove from the set
                currentTable = "";//reset the current table
                System.out.println("Table deleted successfully");
            } catch (SQLException e){
                System.out.println(e.getMessage());
            }
        } else {
            System.out.println("This table does not exist");
        }
    }

    public void deleteEntryById(int id){
        try(PreparedStatement ps = connection.prepareStatement(getDeleteByIdQuery())){//get the prepared query
            ps.setInt(1,id);//insert our input into the query
            int rowsAffected = ps.executeUpdate();//can safely delete invalid ID's, since no error
            if (rowsAffected>0){
                System.out.println("Deleted successfully");
            } else {
                System.out.println("Invalid id. Nothing was deleted");
            }
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public void editEntryNameById(String newName, int id){
        try(PreparedStatement ps = connection.prepareStatement(getUpdateNameByIdQuery())){//get the prepared query
            ps.setString(1,newName);//insert our input into the query
            ps.setInt(2,id);//insert our input into the query
            ps.executeUpdate();
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }
    
    public void editEntryStockById(int newStock, int id){
        try(PreparedStatement ps = connection.prepareStatement(getUpdateStockByIdQuery())){//get the prepared query
            ps.setInt(1,newStock);//insert our input into the query
            ps.setInt(2,id);//insert our input into the query
            ps.executeUpdate();
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public void executeSelectQuery(String query) {
        try (
            PreparedStatement ps = connection.prepareStatement(query);//get the prepared query
            ResultSet rs = ps.executeQuery()//execute and get the result set, which holds data
            ) {
            while (rs.next()){
                System.out.printf("| %s | %-10s | %-5s |%n",rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("stock"));
            }
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public String getCurrentTable(){
        if (!currentTable.isBlank()){
            return currentTable;
        } else {
            return "";
        }
    }

    public void showAllTables() {
        try(Statement statement = connection.createStatement()) {
            for (String table: tables){
                ResultSet rs = statement.executeQuery(getSelectRowCountQuery(table));//get the query and execute
                rs.next();
                if (table.equals(currentTable)){
                    System.out.print("   >");//if found selected table - mark it for convenience
                }
                System.out.printf("|%-20s|%-5d|%n",table,rs.getInt("count"));//get the row count
                rs.close();
            }
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    //method to show and entry to the user, before editing it
    public boolean showEntryIfIdExists(int id){
        try(PreparedStatement ps = connection.prepareStatement(getSelectByIdQuery())) {//get the prepared query
            ps.setInt(1,id);//insert our input into the query
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                System.out.printf("| %s | %-10s | %-5s |%n", 
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("stock"));
                rs.close();
                return true;
            } else {
                System.out.println("Entry not found");
                rs.close();
                return false;
            }
        } catch (SQLException e){
            System.out.println(e.getMessage());
            return false;
        }
    }

    public void insertEntry(String name, int stock){
        try (PreparedStatement ps = connection.prepareStatement(getInsertQuery())){//get the prepared query
            ps.setString(1,name);//insert our input into the query
            ps.setInt(2,stock);//insert our input into the query
            ps.executeUpdate();
            System.out.println("Inserted successfully");
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public String getInsertQuery(){
        return String.format("INSERT INTO %s(name,stock) VALUES(?,?);", currentTable);
    }

    public String getSelectByIdQuery(){
        return String.format("SELECT * FROM %s WHERE id = ?;",currentTable);
    }

    public String getUpdateNameByIdQuery(){
        return String.format("UPDATE %s SET name = ? WHERE id = ?;",currentTable);
    }

    public String getUpdateStockByIdQuery(){
        return String.format("UPDATE %s SET stock = ? WHERE id = ?;", currentTable);
    }

    public String getDeleteByIdQuery(){
        return String.format("DELETE FROM %s WHERE id = ?;",currentTable);
    }

    public String getSelectAllQuery(){
        return String.format("SELECT * FROM %s",currentTable);
    }

    public String getSelectRowCountQuery(String tableName){
        return String.format("SELECT COUNT(*) AS count FROM %s;",tableName);
    }

    public String getCreateTableQuery(String tableName){
        return String.format("CREATE TABLE IF NOT EXISTS %s ("+
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "name text,"+
            "stock INTEGER"+
            ");",tableName);
    }

    public String getDeleteTableQuery(){
        return String.format("DROP TABLE IF EXISTS %s;",currentTable);
    }

}
