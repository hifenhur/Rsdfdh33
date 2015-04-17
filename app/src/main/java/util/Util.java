package util;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.PowerManager;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Util {
	private final static String TAG = "Util";
	 public static PowerManager pm;
	
	
	static String[] Infractions = new String[]{"Tal�o Inv�lido","Tal�o Expirado","Tal�o Incompleto","Tal�o em Branco","Tal�o Adulterado","Sem Tal�o","Estacionado Incorretamente","Tal�o Ileg�vel","Fim Rotatividade","Fim Credito","Fim Tolerancia"};
	
	public static String inputStreamToString(InputStream is) throws IOException {
		InputStreamReader isr = new InputStreamReader(is);
        BufferedReader reader = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
        {
            sb.append(line + "\n");
        }
        return sb.toString();
	}
	
	public static String objectToString(Serializable object) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			new ObjectOutputStream(out).writeObject(object);
			byte[] data = out.toByteArray();
			out.close();

			out = new ByteArrayOutputStream();
			Base64OutputStream b64 = new Base64OutputStream(out, Base64.NO_CLOSE);
			b64.write(data);
			b64.close();
			out.close();

			return new String(out.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Object stringToObject(String encodedObject) {
		if(encodedObject==null) return null;
		try {
			return new ObjectInputStream(new Base64InputStream(new ByteArrayInputStream(encodedObject.getBytes()), Base64.NO_CLOSE)).readObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * Metodo que traduz o id da infra��o
	 * do banco para seu nome no xQRc
	 */
	public static String obterTypeInfractionBy(int index){
		String type_infraction = "";
		switch (index) {
		case 1:
			type_infraction = "Tal�o Inv�lido";
			break;
		case 2:
			type_infraction = "Tal�o Expirado";
			break;
		case 3:
			type_infraction = "Sem Tal�o";
			break;
		case 4:
			type_infraction = "Estacionado Incorretamente";
			break;
		case 5:
			type_infraction = "Tal�o Ileg�vel";
			break;
		case 6:
			type_infraction = "Fim Rotatividade";
			break;
		case 7:
			type_infraction = "Fim Credito";
			break;
		case 8:
			type_infraction = "Fim Tolerancia";
			break;
		case 9:
			type_infraction = "Tal�o em Branco";
			break;
		case 10:
			type_infraction = "Tal�o Incompleto";
			break;
		case 11:
			type_infraction="Tal�o Adulterado";
			break;
		}
		
		return type_infraction;
	}
	/**
	 * Metodo que seleciona o tipo de infra��o no
	 * listBox do agente. Abaixo esta o mapeamento
	 * 
	 * ListBox do agente				Banco de dados						Mapeamento no metodo: Banco(case):metodo(return)
       *Tal�o incompleto - 1				10;"Tal�o incompleto"				10-1
        *Tal�o em branco - 2				9;"Tal�o em branco"					9-2
        *Tal�o Inv�lido - 3 				1;"Cart�o Inv�lido"					1-3
        *Tal�o Expirado - 4					2;"Cart�o Expirado"					2-4
        *Tal�o Adulterado - 5				11;	"Tal�o Adulterado"				11-5				---
        *Sem tal�o - 6						3;"Sem Cart�o"						3-6
        *Estacionado incorretamente - 7		4;"Estacionado Incorretamente"		4-7
        *Tal�o ilegivel - 8					5;"Cart�o Ileg�vel"					5-8
        *Fim de rotatividade -9				6;"Fim Rotatividade"				6-9
        *Fim Credito - 10					7;"Fim Credito"						7-10
        *Fim Tolerancia - 11				8;"Fim Tolerancia"					8-11
	 */

	public static int arrayAgentInfracaoPosition(int cod)
	{
		switch (cod) {
		
			case 1://Numero no banco
			return 3;//Tal�o Inv�lido
			
			case 2://Numero no banco
				return 4;//Tal�o Expirado
				
			case 3://Numero no banco
				return 6;//Sem Cart�o
				
			case 4://Numero no banco
				return 7;//Estacionado incorretamente
				
			case 5://Numero no banco
				return 8;//Tal�o ilegivel
				
			case 6://Numero no banco
				return 9;//Fim de rotatividade
				
			case 7://Numero no banco
				return 10;//Fim Credito
				
			case 8://Numero no banco
				return 11;//Fim Tolerancia
				
			case 9://Numero no banco
				return 2;//Tal�o em branco
				
			case 10://Numero no banco
				return 1;//Tal�o incompleto
			case 11://Numero no banco
				return 5;//Tal�o Adulterado
				
		default:
			return 0;
		}
		
	}
	/**
	 * Obtem o numero da infra��o pelo seu nome
	 * Importante que os nomes estejam escritos no 
	 * listBox do agente da mesma forma que est� escrito 
	 * nesse metodo
	 */
	public static int obterTypeInfractionByInfraction(String infraction){
		if(infraction.equals("Tal�o Inv�lido")) return 1; 
		if(infraction.equals("Tal�o Expirado")) return 2;
		if(infraction.equals("Sem Tal�o")||(infraction.equals("Sem tal�o"))) return 3;
		if(infraction.equals("Estacionado Incorretamente")||(infraction.equals("Estacionado incorretamente"))) return 4;
		if(infraction.equals("Tal�o Ileg�vel")||(infraction.equals("Tal�o ilegivel"))) return 5;
		if(infraction.equals("Fim Rotatividade")||(infraction.equals("Fim de rotatividade"))) return 6;//ok
		if(infraction.equals("Fim Credito")) return 7;//ok
		if(infraction.equals("Fim Tolerancia")) return 8;//ok
		if(infraction.equals("Tal�o em branco")||(infraction.equals("Tal�o em Branco"))) return 9;//ok
		if(infraction.equals("Tal�o incompleto")||(infraction.equals("Tal�o Incompleto"))) return 10;//ok
		if(infraction.equals("Tal�o Adulterado")) return 11;
		return -1;
	}
	/**
	 * Metodo que insere as infra��es no listBox 
	 * do monitor
	 */
	public static String[] obterRangeofStrings(int begin,int end){
		String[] NewArray = new String[(end-begin) + 1];
		NewArray[0] = "Selecione uma op��o";
		int j = 1; //reseter
		try{
			for(int i = begin;i < end; i++){
				NewArray[j] = Infractions[i];
				j++;
			}
		}catch(ArrayIndexOutOfBoundsException ex){
			Log.i(TAG, "ERROR " + ex.getMessage());
		}catch(Exception ex){
			Log.i(TAG, "ERROR " + ex.getMessage());
		}
		
		return NewArray;
	}
	/**
	 * Metodo que mostra um aviso na tela
	 */
	public static void aviso(String title,String msg, Activity screem)
	{
		AlertDialog.Builder dlg;
		dlg = new AlertDialog.Builder(screem);
		dlg.setTitle(title);
		dlg.setMessage(msg);
		dlg.setPositiveButton("OK", null);
		dlg.show();
	}
	public static String getCurrencyDate()
	{
	        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
	        Calendar c = Calendar.getInstance();
	       
	        return sdf.format(c.getTime()).toString();
	}
	/**
	 * Pega da hora atual
	 */
	public static String getCurrencyHour(){
		
		Calendar c = Calendar.getInstance();
		return timeFormat(c.get(Calendar.HOUR_OF_DAY))+":"+timeFormat(c.get(Calendar.MINUTE));
	}
	public static String timeFormat(int i)
	{
		if(i<10)
		{
			return "0"+i;
		}
		else
		{
			return ""+i;
		}
	}
	 public static String hourTime(long seconds)
		{
		 String signal="";
		 if(seconds<0)
		 {
			 seconds=seconds*(-1);
			 signal="-";
		 }
			int hour=(int) (seconds/3600);
			int minutes=(int) (seconds/60-hour*60);
			int seg=(int) (seconds-minutes*60-hour*3600);
			return signal+" "+timeFormat(hour)+":"+timeFormat(minutes)+":"+timeFormat(seg);
		}


}
