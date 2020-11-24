package xTestes;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;

import com.sankhya.util.TimeUtils;

public class teste {

	public static void main(String[] args) {
		Timestamp now = TimeUtils.getNow();
		Timestamp now2 = new Timestamp(System.currentTimeMillis());
		Instant now3 = Instant.now();
		ZonedDateTime now4 = ZonedDateTime.now();
		System.out.println(now);
	}
}
