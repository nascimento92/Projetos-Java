package br.com.Desenvolvimentos;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

import com.sankhya.util.StringUtils;
import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class eventoAtividadesProjetos implements EventoProgramavelJava {

	public void afterDelete(PersistenceEvent arg0) throws Exception {
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		BigDecimal idProjeto = newVO.asBigDecimal("ID");
		BigDecimal idEtapa = newVO.asBigDecimal("NROETAPA");
		
		double soma = soma(idProjeto,idEtapa);
		salvaTotalHoras(idProjeto,idEtapa, soma);
		
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {	
		
	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		
		Timestamp dtFinal = newVO.asTimestamp("DTFIM");
		BigDecimal idProjeto = newVO.asBigDecimal("ID");
		BigDecimal idEtapa = newVO.asBigDecimal("NROETAPA");
		
		salvaResponsavel(arg0);
		
		if(dtFinal!=null) {
			
			double soma = soma(idProjeto,idEtapa);
			salvaTotalHoras(idProjeto,idEtapa, soma);
		}
		
	}

	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		insert(arg0);
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		Timestamp dtInicio = newVO.asTimestamp("DTINICIO");
		Timestamp dtFinal = newVO.asTimestamp("DTFIM");
		
		if(dtFinal!=null) {
			if(dtInicio.after(dtFinal)) {
				throw new Error("<br/> <b>Data inicial não pode ser maior que data final!</b> <br/>");
			}
			
			double horasTrabalhadas = diferencaEmHoras(dtInicio,dtFinal);
			BigDecimal a = new BigDecimal(horasTrabalhadas);
			BigDecimal b = casasDecimais(2, a);
			newVO.setProperty("TEMPO", b);
		}
	}
	
	
	private void insert(PersistenceEvent arg0) throws Exception {
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		
		Timestamp dtInicio = newVO.asTimestamp("DTINICIO");
		Timestamp dtFinal = newVO.asTimestamp("DTFIM");
		BigDecimal idProjeto = newVO.asBigDecimal("ID");
		BigDecimal idEtapa = newVO.asBigDecimal("NROETAPA");
		
		salvaResponsavel(arg0);
		
		if(dtFinal!=null) {
			
			if(dtInicio.after(dtFinal)) {
				throw new Error("<br/> <b>Data inicial não pode ser maior que data final!</b> <br/>");
			}
			
			double horasTrabalhadas = diferencaEmHoras(dtInicio,dtFinal);
			BigDecimal a = new BigDecimal(horasTrabalhadas);
			BigDecimal b = casasDecimais(2, a);
			newVO.setProperty("TEMPO", b);
			
			double soma = soma(idProjeto,idEtapa);
			double valorfinal = soma+horasTrabalhadas;
			
			salvaTotalHoras(idProjeto,idEtapa, valorfinal);
		}
		
	}
	
	private void salvaResponsavel(PersistenceEvent arg0) {
		DynamicVO newVO = (DynamicVO) arg0.getVo();	
		newVO.setProperty("RESPONSAVEL", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
	}
	
	private double soma(BigDecimal idProjeto, BigDecimal idEtapa) {
		
		double total=0;
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_ATIVIDADEPROJETO","this.ID = ? AND this.NROETAPA=?", new Object[] { idProjeto,idEtapa }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			BigDecimal tempo = (BigDecimal) DynamicVO.getProperty("TEMPO");
			double x = tempo.doubleValue();
			
			total+=x;

			}

		} catch (Exception e) {
			salvarException("[soma] nao foi possivel somar as horas! "+e.getMessage()+"\n"+e.getCause());
		}
		
		return total;
	}
	
	private void salvaTotalHoras(BigDecimal idProjeto, BigDecimal idEtapa, double horas) throws Exception {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_ETAPAPROJETO",
					"this.ID=? AND this.NROETAPA=? ", new Object[] { idProjeto, idEtapa }));
			
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("TEMPO", new BigDecimal(horas));

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			salvarException("[salvaTotalHoras] nao foi possivel salvar as horas! "+e.getMessage()+"\n"+e.getCause());
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
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "eventoAtividadesProjetos");
			VO.setProperty("PACOTE", "br.com.Desenvolvimentos");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}
	
	
}
