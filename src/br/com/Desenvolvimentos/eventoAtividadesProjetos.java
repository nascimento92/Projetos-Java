package br.com.Desenvolvimentos;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Locale;

import com.sankhya.util.StringUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;

public class eventoAtividadesProjetos implements EventoProgramavelJava {

	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		start(arg0);
	}
	
	private void start(PersistenceEvent arg0) throws Exception {
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		
		Timestamp newDataFinal = newVO.asTimestamp("DTFIM");
		Timestamp oldDataFinal = oldVO.asTimestamp("DTFIM");
		
		Timestamp dtInicio = newVO.asTimestamp("DTINICIO");
				
		if(newDataFinal!=null && oldDataFinal==null) {
			newVO.setProperty("STATUS", "2");
			
			double horasTrabalhadas = diferencaEmHoras(dtInicio,newDataFinal);
			BigDecimal a = new BigDecimal(horasTrabalhadas);
			BigDecimal b = casasDecimais(2,a);
			newVO.setProperty("TEMPO", b);
		}
		
		if(newDataFinal!=oldDataFinal && newDataFinal!=null) {
			double horasTrabalhadas = diferencaEmHoras(dtInicio,newDataFinal);
			BigDecimal a = new BigDecimal(horasTrabalhadas);
			BigDecimal b = casasDecimais(2,a);
			newVO.setProperty("TEMPO", b);
		}
		
		if(newDataFinal==null && oldDataFinal!=null) {
			newVO.setProperty("TEMPO", null);
			newVO.setProperty("STATUS", "1");
		}
		
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
	
	private BigDecimal casasDecimais(int casas, BigDecimal valor)
	{
	    String quantCasas = "%."+casas+"f", textoValor = "0";
	    try
	    {
	        textoValor = String.format(Locale.getDefault(), quantCasas, valor);
	    }catch(java.lang.IllegalArgumentException e)
	    {
	        // Quando os digitos com 2 casas decimais forem Zeros, exemplo: 0.000001233888.
	        // Nao existe valor 0,00 , logo entra na java.lang.IllegalArgumentException.
	        if(e.getMessage().equals("Digits < 0"))
	            textoValor = "0";
	        System.out.println(e.getMessage());
	    }
	    return new BigDecimal(textoValor.replace(",", "."));
	}
	
	
}
