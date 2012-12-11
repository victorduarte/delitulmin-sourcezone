package org.source;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
/**
 * Clase para la creación de la barra de progreso en Shell.
 * 
 *   <ul>Utilización de la clase :
 *    <li>(A)Obtenemos la barra:
 *              <ul>
 *                 <li>ProgressBarShell.createProgressBar(message);</li>
 *              </ul>
 *    </li>
 *    <li>
 *        (B)Realizamos la tarea a ejecutar
 *           <ul>
 *             <li>do execute jobs ... </li>
 *           </ul>
 *    </li>
 *    <li>(C)Finalizamos la barra de progreso:
 *       <ul>
 *         <li>Para ello: ProgressBarShell.finish(messageFinish);</li>
 *         <li>Si queremos encadenar barras de progreso el message que recibe el createProgressBar(message)
 *             deberemos obtenerlo una vez ejecutado ProgressBarShell.finish(messageFinish) + "texto a incluir en la nueva barra".
 *         </li>
 *       </ul>
 *    </li>
 *   </ul>
 * 
 * @author victor.duarte 
 * @email  delitulmin@gmail.com
 */
public class ProgressBarShell {
	
	/* ==========================================================
	 * Parámetros estáticos para ProgressBarShell
	 * ==========================================================*/
	
	// Salida estandar.
	private static PrintStream print = System.out;
	
	// Tamaño de la barra de progreso.
	private static final int MAX_POINTER = 60;
	
	// Caracteres para la barra de progreso.
	private static final char CHARACTER_BEGIN_BAR      ='[';
	private static final char CHARACTER_END_BAR        =']';
	private static final char CHARACTER_BEGIN_PROGRESS = '-';
	private static final char CHARACTER_END_PROGRESS   = '|';
	
	// Mensajes a mostrar.
	private static String MESSAGE_MAIN   = "It's executing:\n\n";
	private static String MESSAGE_FINISH = "Terminated:\n\n";
	private static String MESSAGE_RETURN = null;
		
	private InnerProgressBar createInnerProgressBar () 
	{return new InnerProgressBar();}
	
	private InnerProgressBar createInnerProgressBar (String message) 
	{return new InnerProgressBar(message);}
	
	private static InnerProgressBar monitorPbs = null;
	public synchronized static void createProgressBar ( String message ) throws Exception 
	{	
		if (monitorPbs!=null) 
			throw new RuntimeException(ProgressBarShell.class.getName() + " is already running ...");

		MESSAGE_RETURN = null;
		if ( message == null || message.isEmpty() ) {
			monitorPbs = new ProgressBarShell().createInnerProgressBar();
		} else {
			monitorPbs = new ProgressBarShell().createInnerProgressBar( message );
		}
		Thread th = new Thread( monitorPbs );
		th.start();
	}
	
	public synchronized static String finish ( String message ) throws Exception
	{
		if (monitorPbs==null) 
			throw new RuntimeException ( ProgressBarShell.class.getName() + " is not running ..." );
		
		monitorPbs.doFinish(message);
		monitorPbs.doMessageFinish();
		monitorPbs = null;
		return MESSAGE_RETURN;
	}
	
	/* =====================================================================
	 * Clase interna para la generación de la barra de progreso en el Shell.
	 * =====================================================================
	 * */
	private class InnerProgressBar implements Runnable 
	{	
		// Monitor para la sincronización.
		private Object monitor = new Object();
			
		//Contenido en byte del shell limpiado.
		private ByteArrayOutputStream clearShell = null;
		
		private InnerProgressBar() {}
		private InnerProgressBar(String message){MESSAGE_MAIN = message + "\n";}
		
		private boolean start = true;
		private void doFinish ( String messageFinish ) 
		{
			synchronized (monitor) {
				MESSAGE_FINISH = messageFinish;
				this.start = false;
			}
		}

		private String doMessageFinish () throws Exception 
		{
			while (MESSAGE_RETURN == null) {
				synchronized (monitor) {
					monitor.wait(500);
				}
			}
			return MESSAGE_RETURN;
		}
		
		@Override
		public void run() {
			
			int points = 1;
			
			//1.Limpiamos la pantalla
			try {
				refreshProgressBar();
			} catch (Exception ex) {
				closeShell();
				throw new RuntimeException(ex);
			}
			
			//2.Creamos la barra de progreso
			char[] progressBarArray = doProgressBar(CHARACTER_BEGIN_PROGRESS, "  0%");
			
			//3.Avanzamos la barra de progreso
			while ( start ) {
				try { 
					synchronized ( monitor ) 
					{
						if (points > MAX_POINTER ) {
							//3A.Si llegamos al maximo comenzamos de nuevo
							progressBarArray = resume();
							points = 1;
						} else {
							//3B.Realizamos el progreso de la barra
							advanceProgressBar(points, progressBarArray);
							points++;
						}
						monitor.wait(100);
					}
				} catch (Exception ex) {
					closeShell();
					throw new RuntimeException(ex);
				}
			}
			
			//4.Antes de salir volvemos a la barra original
			try {
				String textProgressBar = finishProgressBar();
				MESSAGE_RETURN = MESSAGE_MAIN + textProgressBar + "\n" + MESSAGE_FINISH; 
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			} finally {
				closeShell();
			}
			print.print("\n");
		}
		
		private void refreshProgressBar() throws IOException 
		{
			doClearShell();
			print.write(this.clearShell.toByteArray());
			print.print(MESSAGE_MAIN);
		}
		
		private String finishProgressBar() throws IOException 
		{
			refreshProgressBar();
			char[] progressBarArray = doProgressBar(CHARACTER_END_PROGRESS, "100%");
			
			String textProgressBar = new String(progressBarArray);
			print.println(textProgressBar);
			print.println(MESSAGE_FINISH);
			return textProgressBar;
		}
		
		private void advanceProgressBar(int points, char[] progressBarArray) throws IOException 
		{
			refreshProgressBar();			
			progressBarArray[points] = CHARACTER_END_PROGRESS;

			//Calculamos el porciento
			double percent  = percent(points,0);
			
			//Actualizamos el porciento en el texto
			String textFine = new String(progressBarArray);
			int lastClasp   = textFine.lastIndexOf(CHARACTER_END_BAR);
			textFine        = textFine.substring(0, lastClasp+1);
			textFine       += " "+percent+"%";
			
			print.println(textFine);
		}
		
		private char[] resume() throws IOException 
		{
			refreshProgressBar();
			char[] progressBarArray = doProgressBar(CHARACTER_BEGIN_PROGRESS, "  0%");
			print.print(new String(progressBarArray));
			return progressBarArray;
		}
		
		private void closeShell() 
		{
			try {
				this.clearShell.close();
				this.clearShell=null;
			} catch (IOException e) {
				this.clearShell=null;
			}
		}
		
		private double percent( double number, int decimal ) 
		{
			double percent = ((float)number/(float)MAX_POINTER)*100;
			long mult      = (long)Math.pow(10,decimal);
			return (Math.round(percent*mult))/(double)mult;
		} 
		
		/*
		 * Creamos la barra de progreso
		 */
		private char[] doProgressBar( char caracter, String fin ) 
		{
			StringBuffer sb = new StringBuffer();
			sb.append(CHARACTER_BEGIN_BAR);
			for (int i = 0; i < MAX_POINTER; i++) {
				sb.append(caracter);
			}
			sb.append(CHARACTER_END_BAR);
			sb.append(" " + fin);
			return sb.toString().toCharArray();
		}
		
		/*
		 * Nos permite limpiar la ventana del shell.
		 */
		private void doClearShell() throws IOException 
		{
			if (this.clearShell!=null)return;
			
			BufferedInputStream buffer = null;
			try {
				String so      = System.getProperty("os.name");
				String command = null;
				if (so.equals("Windows")) {
					command = "cls";
				} else {
					command = "clear";
				}
				
				Process exec    = Runtime.getRuntime().exec(command);
				buffer          = new BufferedInputStream(exec.getInputStream());
				this.clearShell = new ByteArrayOutputStream();
				
				int c;
				while ((c = buffer.read()) != -1) {
					this.clearShell.write(c);
				}
			} catch (Exception ex) {
				throw new IOException(ex.getMessage());
		    } finally {
				if (buffer!=null)buffer.close();
			}
		}
	}
}
