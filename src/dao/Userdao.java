package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import model.Staff;
import model.Student;
import model.User;

public class Userdao {

    public static String login(String id, String password) throws Exception {
        PreparedStatement ps = Database.getInstance().get()
                .prepareStatement("SELECT name FROM users WHERE id=? AND password=?");
        ps.setString(1, id);
        ps.setString(2, password);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getString("name") : null;
    }

    public static boolean register(String id, String name, String password, String type, boolean final_year) throws Exception{

            PreparedStatement ps = Database.getInstance().get()
                    .prepareStatement("INSERT INTO users (id, name, password, type, final_year) VALUES (?, ?, ?, ?, ?)");
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, password);
            ps.setString(4, type);
            ps.setInt(5, final_year ? 1 : 0);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
    }
    public static User getUserById(String id) throws Exception {
        PreparedStatement ps = Database.getInstance().get()
                .prepareStatement("SELECT * FROM users WHERE id=?");
        ps.setString(1, id);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) return null;

        String name = rs.getString("name");
        String type = rs.getString("type");
        boolean finalYear = rs.getInt("final_year") == 1;

        if (type.equals("STAFF")) {
                return new Staff(id, name);
        } else {
                return new Student(id, name, finalYear);
        }
    }
}
