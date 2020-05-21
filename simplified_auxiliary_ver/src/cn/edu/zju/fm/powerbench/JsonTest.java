package cn.edu.zju.fm.powerbench;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import com.google.gson.Gson;

public class JsonTest {

	public static void main(String[] args) throws FileNotFoundException {
		File file = new File("E:/STM32/hex/test.json");
		FileReader fr = new FileReader(file);
		Gson gson = new Gson();
		Settings settings;
		settings = gson.fromJson(fr, Settings.class);
		System.out.println(settings);
	}

}
