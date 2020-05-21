package test.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.google.gson.Gson;

class JsonParseException extends Exception {
	private static final long serialVersionUID = -4061901483701085764L;}

public class AutoTest {
	
	private Settings settings;
	
	public AutoTest(File jsonFile) throws FileNotFoundException, JsonParseException {
		FileReader fr = new FileReader(jsonFile);
		Gson gson = new Gson();
		try {
			settings = gson.fromJson(fr, Settings.class);
		} catch (Exception e) {
			e.printStackTrace();
			throw new JsonParseException();
		} finally {
			try { fr.close(); } catch (IOException e) { }
		}
		System.out.println(settings.toString());
	}
	
	private void test() {
		for (TestCase testcase: settings.testCases) {
			testcase.test();
		}
		System.out.println("测试内容,平均电流(mA),平均时间(s),执行次数,总平均功耗(mJ),单次功耗(mJ),测试次数,数据");
		for (TestCase testcase: settings.testCases) {
			if (testcase.canCalc) {
			//if (true) {
				try {
					System.out.println(testcase.calc());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void main(String[] args) {
		try {
			AutoTest auto = new AutoTest(new File("E:/STM32/hex/test.json"));
			auto.test();
		} catch (FileNotFoundException e) {
			System.out.println("Can't not find settings file, or can not open.");
			e.printStackTrace();
		} catch (JsonParseException e) {
			System.out.println("Json file parse err.");
		}
	}

}
