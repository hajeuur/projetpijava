import com.mentorai.utils.MyConnection;
import java.sql.SQLException;

public class TestDB {
    public static void main(String[] args) {
        try {
            MyConnection.getInstance();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}