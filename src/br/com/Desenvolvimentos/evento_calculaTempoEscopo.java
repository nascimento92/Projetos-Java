package br.com.Desenvolvimentos;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Calendar;

import com.sankhya.util.StringUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;

public class evento_calculaTempoEscopo implements EventoProgramavelJava{

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		calculaDatas(arg0);		
	}
	
	public void calculaDatas(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		Timestamp dataFinal = VO.asTimestamp("DTFIM");
		Timestamp dataInicio = VO.asTimestamp("DTINICIO");
		
		double resultado = diferencaEmHoras(dataInicio,dataFinal);
		
		VO.setProperty("TEMPO", new BigDecimal(resultado));
	}
	
	public double diferencaEmHoras(Timestamp dataInicio, Timestamp dataFim) {
		
		String dataInicioApenasHoras = StringUtils.formatTimestamp(dataInicio, "HH:mm:ss");
		String dataFimApenasHoras = StringUtils.formatTimestamp(dataFim, "HH:mm:ss");
		
		String[] horaEntradaHMS = dataInicioApenasHoras.split(":");
		String[] horaSaidaHMS = dataFimApenasHoras.split(":");
		Calendar calHorasTrabalhadas = Calendar.getInstance();
		
		// Set hora saida
		calHorasTrabalhadas.set(Calendar.HOUR, Integer.parseInt(horaSaidaHMS[0]));
		calHorasTrabalhadas.set(Calendar.MINUTE, Integer.parseInt(horaSaidaHMS[1]));
		calHorasTrabalhadas.set(Calendar.SECOND, Integer.parseInt(horaSaidaHMS[2]));
		
		// Subtrai as horas entrada
		calHorasTrabalhadas.add(Calendar.HOUR, (-1) * Integer.parseInt(horaEntradaHMS[0]));
		calHorasTrabalhadas.add(Calendar.MINUTE, (-1) * Integer.parseInt(horaEntradaHMS[1]));
		calHorasTrabalhadas.add(Calendar.SECOND, (-1) * Integer.parseInt(horaEntradaHMS[2]));
				
		double segundos = calHorasTrabalhadas.get(Calendar.SECOND);
		double minutos = calHorasTrabalhadas.get(Calendar.MINUTE);
		double horas = calHorasTrabalhadas.get(Calendar.HOUR);
		
		double tempoCorreto = ((segundos/60)/60)+(minutos/60)+horas;
		double horaFinal = converterDoubleDoisDecimais(tempoCorreto);
		
		return horaFinal;
	}
	
	public double converterDoubleDoisDecimais(double precoDouble) {
	    DecimalFormat fmt = new DecimalFormat("0.00");      
	    String string = fmt.format(precoDouble);
	    String[] part = string.split("[,]");
	    String string2 = part[0]+"."+part[1];
	        double preco = Double.parseDouble(string2);
	    return preco;
	}

}
