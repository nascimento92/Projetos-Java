package br.com.Desenvolvimentos;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class eventoCalculaTempoDasAtividades implements EventoProgramavelJava {

	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		registraTempoTotalDasEtapas(arg0);
		
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
		DynamicVO VO = (DynamicVO) arg0.getVo();
		Timestamp dataFinal = VO.asTimestamp("DTFIM");
		
		if(dataFinal!=null) {
			BigDecimal nroEtapa = VO.asBigDecimal("NROETAPA");
			BigDecimal id = VO.asBigDecimal("ID");
			
			double tempo = calculaTempoTotal(nroEtapa,id);
			BigDecimal a = new BigDecimal(tempo);
			BigDecimal b = casasDecimais(3,a);
			
			VO.setProperty("TEMPO", b);
			VO.setProperty("STATUS", "4");
			
			//throw new PersistenceException("\n\nTempo: "+tempo+"\nBigDecimal: "+a+"\nBig Convertido: "+b);
		}
	}
	
	private double calculaTempoTotal(BigDecimal nroEtapa,BigDecimal id) throws Exception {
		double tempo = 0.0;
		double valor = 0.0;
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		Collection<?> col = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_ATIVIDADEPROJETO","this.NROETAPA=? AND this.ID=? ", new Object[] { nroEtapa,id }));
		for (Iterator<?> Iterator = col.iterator(); Iterator.hasNext();) {

		PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
		DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
		
		Timestamp fim = DynamicVO.asTimestamp("DTFIM");
		
			if (fim == null) {
				throw new PersistenceException(
						"\n\nA atividade " + DynamicVO.asBigDecimal("NRATIVIDADE") + " nao foi finalizada!\n\n");
			} else if (fim != null) {
				valor = DynamicVO.asDouble("TEMPO");
				tempo += valor;
			}

		}
		return tempo;
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
	
	public double converterDoubleDoisDecimais(double precoDouble) {
	    DecimalFormat fmt = new DecimalFormat("0.00");      
	    String string = fmt.format(precoDouble);
	    String[] part = string.split("[,]");
	    String string2 = part[0]+"."+part[1];
	        double preco = Double.parseDouble(string2);
	    return preco;
	}
	
	public void registraTempoTotalDasEtapas(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		Timestamp tempoFinal = VO.asTimestamp("DTFIM");
		
		if(tempoFinal!=null) {
			BigDecimal id = VO.asBigDecimal("ID");
			double tempo = calculaTempoTotal(id);
			BigDecimal a = new BigDecimal(tempo);
			BigDecimal b = casasDecimais(3,a);
			
			salvaDados(b,id);
		}
	}
	
	private double calculaTempoTotal(BigDecimal id) throws Exception {
		double tempo = 0.0;
		double valor = 0.0;
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		Collection<?> col = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_ETAPAPROJETO","this.ID=? ", new Object[] { id }));
		for (Iterator<?> Iterator = col.iterator(); Iterator.hasNext();) {

		PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
		DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
		
		Timestamp fim = DynamicVO.asTimestamp("DTFIM");
		
			if (fim != null) {
				valor = DynamicVO.asDouble("TEMPO");
				tempo += valor;
			}
		}
		return tempo;
	}
	
	private void salvaDados(BigDecimal tempo, BigDecimal id) throws Exception {
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity PersistentLocalEntity = dwfFacade.findEntityByPrimaryKey("AD_PROJETOS", id);
		EntityVO NVO = PersistentLocalEntity.getValueObject();
		DynamicVO appVO = (DynamicVO) NVO;

		appVO.setProperty("TEMPATUAL", tempo);

		PersistentLocalEntity.setValueObject(NVO);

	}
	
}
