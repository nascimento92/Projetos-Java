package xTestes;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.sankhya.util.TimeUtils;

public class somar_tempo {
	public static void main(String[] args) throws InterruptedException {
		Timestamp horaAtual = new Timestamp(System.currentTimeMillis());
		System.out.println(horaAtual);
		
		Timestamp hora10minutos = addMinutes(horaAtual, new BigDecimal(1));
		
		System.out.println(hora10minutos);
		
		System.out.println("Adicionando 7 dias: "+TimeUtils.dataAddDay(TimeUtils.getNow(), 7));
		System.out.println("Adicionando 7 dias úteis: "+ new Timestamp(TimeUtils.addWorkingDays(TimeUtils.getNow().getTime(), 7)));
		
		System.out.println("Agora: "+TimeUtils.getNow());
		System.out.println("Diferença em minutos entre as datas: "+TimeUtils.getDifferenceInMinutes(TimeUtils.getNow(), addMinutes(horaAtual, new BigDecimal(3))));
		
		long hrinicial = TimeUtils.getNow().getTime();
		new Thread().sleep(2000); //aguardar 2 segundos
		long hrfinal = TimeUtils.getNow().getTime();
		long diferenca = hrfinal - hrinicial;
		long diferencaSeg = diferenca / 1000;    //DIFERENCA EM SEGUNDOS   
		long diferencaMin = diferenca / (60 * 1000);    //DIFERENCA EM MINUTOS   
		long diferencaHoras = diferenca / (60 * 60 * 1000);    // DIFERENCA EM HORAS 
		
		System.out.println("Diferença: "+ diferenca / 1000);
	}
	
	
	private static Timestamp addMinutes(Timestamp datainicial,BigDecimal prazo){
		GregorianCalendar gcm = new GregorianCalendar();
		Date data = new Date(datainicial.getTime());
		gcm.setTime(data);
		gcm.add(Calendar.MINUTE, prazo.intValue());
		data = gcm.getTime();
		Timestamp dataInicialMaisPrazo = new Timestamp(data.getTime());
		
		return dataInicialMaisPrazo;
	}
}


