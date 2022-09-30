package xTestes;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;

import com.sankhya.util.TimeUtils;

public class testando {
	public static void main(String[] args) {
		
		Timestamp a = TimeUtils.getNow();
		
		LocalDateTime now = a.toLocalDateTime();
		LocalDateTime dt2 = now.plusHours(1).plusMinutes(30);
		
		Duration duration = Duration.between(now, dt2);
		long diff = Math.abs(duration.toMinutes());
		
		double r = Double.valueOf(diff) / 60;
		
		System.out.println("AGORA: "+now);
		System.out.println("PROXIMA DATA: "+dt2);
		System.out.println("DURATION MINUTES: "+diff);
		System.out.println("RESULT: "+r);
		
		int dia = 1;
		int mes = 2;
		int ano = 2022;
		
		System.out.println(TimeUtils.buildData(dia, mes, ano));
	}
	
	private double diff (Timestamp dtinicial, Timestamp dtfinal) {
		LocalDateTime a = dtinicial.toLocalDateTime();
		LocalDateTime b = dtfinal.toLocalDateTime();
		
		Duration duration = Duration.between(a, b);
		long diff = Math.abs(duration.toMinutes());
		double result = Double.valueOf(diff) / 60;
		
		return result;
	}
	
	
}
