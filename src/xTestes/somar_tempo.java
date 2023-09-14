package xTestes;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.sankhya.util.TimeUtils;

public class somar_tempo {
	public static void main(String[] args) {
		Timestamp horaAtual = new Timestamp(System.currentTimeMillis());
		System.out.println(horaAtual);
		
		Timestamp hora10minutos = addMinutes(horaAtual, new BigDecimal(1));
		
		System.out.println(hora10minutos);
		
		System.out.println("Adicionando 7 dias: "+TimeUtils.dataAddDay(TimeUtils.getNow(), 7));
		System.out.println("Adicionando 7 dias úteis: "+ new Timestamp(TimeUtils.addWorkingDays(TimeUtils.getNow().getTime(), 7)));
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


