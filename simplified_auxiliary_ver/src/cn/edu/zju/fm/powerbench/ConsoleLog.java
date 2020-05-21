package cn.edu.zju.fm.powerbench;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.eclipse.ui.console.MessageConsoleStream;

public class ConsoleLog {

	public static void print(String message) {
		String time = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]")
				.format(Calendar.getInstance().getTime());
		System.out.println(time + " " + message);
		System.out.flush();
		
	}
	
	public static void main(String[] args) {
		print("test");
		MessageConsoleStream printer = ConsoleFactory.getConsole()
				.newMessageStream();
		printer.setActivateOnWrite(true);
		printer.println("Ã· æ£∫" + "haha" );
	}
	
}
