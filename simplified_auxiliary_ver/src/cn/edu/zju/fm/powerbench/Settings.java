package cn.edu.zju.fm.powerbench;

public class Settings {
	public TestCase[] testCases;
	
	@Override
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		for (TestCase tc : testCases) {
			strbuf.append(tc.toString());
			strbuf.append("\n");
			strbuf.append("#############################################");
			strbuf.append("\n");
		}
		return strbuf.toString();
	}
}
