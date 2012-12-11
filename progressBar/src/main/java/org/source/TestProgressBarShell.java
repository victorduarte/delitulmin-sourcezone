package org.source;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class TestProgressBarShell {

	public static void main ( String[] args )throws Exception 
	{
		PrintWriter print = new PrintWriter(System.out);
		BufferedReader in  = 
				new BufferedReader(new InputStreamReader(System.in));
		
		String message = "Execute test";
		
		ProgressBarShell.createProgressBar(message);
		
		boolean condition = true;
		while ( condition )
		{	
			print.printf("> ");
			
			//waiting to enter...
			in.readLine().trim();
			
			ProgressBarShell.finish("Finished test");
			condition = false;
		}
	}
}
