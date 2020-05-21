package Serial;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ConsoleLog {

	public static void print(String message) {
		String time = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]")
				.format(Calendar.getInstance().getTime());
		System.out.println(time + " " + message);
		System.out.flush();
	}
	
	public static void main(String[] args) {
		print("test");
	}
	
}
