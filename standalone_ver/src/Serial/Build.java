package Serial;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Build {
	
	private String command;
	private File execDir;
	private String stdout = null;
	private String stderr = null;
	private int ret = -1;
	
	public Build(String compiler, String args, String execDir) {
		command = compiler + " " + args;
		this.execDir = new File(execDir);
	}
	
	public boolean isValidDirectory() {
		return execDir.isDirectory();
	}
	
	public int getReturnValue() {
		return ret;
	}
	
	public String getStdout() {
		return stdout;
	}
	
	public String getStderr() {
		return stderr;
	}
	
	public void run() {
		String s;
		ret = -1;
		stdout = null;
		stderr = null;
		try {
			Process p = Runtime.getRuntime().exec(command, null, execDir);
			BufferedReader stdOutput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			ret = p.waitFor();
			while ((s = stdOutput.readLine()) != null) {
				if (stdout == null) {
					stdout = new String(s);
				} else {
					stdout = stdout + "\n" + s;
				}
			}
			while ((s = stdError.readLine()) != null) {
				if (stderr == null) {
					stderr = new String(s);
				} else {
					stderr = stderr + "\n" + s;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void print() {
		System.out.print("return value: ");
		System.out.println(ret);
		
		System.out.println();
		System.out.println("stdout:");
		System.out.println(stdout);
		
		System.out.println();
		System.out.println("stderr:");
		System.out.flush();
		System.err.println(stderr);
	}

	public static void main(String[] args) {
		
		String compiler = "C:\\Program Files (x86)\\Dev-Cpp\\MinGW32\\bin\\gcc";
		String arg = "math.c";
		String dir = "C:\\Users\\tony\\Desktop\\tmp";
		
		Build build = new Build(compiler, arg, dir);
		build.run();
		build.print();
	}

}
