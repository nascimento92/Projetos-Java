package br.com.Desenvolvimentos;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

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

public class eventoCalculaTempoDasAtividades implements EventoProgramavelJava {

	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		aUpdate(arg0);
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
		bUpdate(arg0);
	}

	private void bUpdate(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		Timestamp dataFinal = VO.asTimestamp("DTFIM");
		Timestamp dataInicio = VO.asTimestamp("DTINICIO");
		String status = VO.asString("STATUS");
		
		if ("2".equals(status) && dataInicio == null) {
			VO.setProperty("DTINICIO", TimeUtils.getNow());
		}

		if ("3".equals(status) && dataFinal == null) {
			VO.setProperty("DTFIM", TimeUtils.getNow());
		}
		
		if(dataFinal!=null) {
			if (dataInicio.after(dataFinal)) {
				throw new Error("<br/> <b>Data inicial não pode ser maior que data final!</b> <br/>");
			}
		}

	}

	private void aUpdate(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal idProjeto = VO.asBigDecimal("ID");
		double somaTempoPrevisto = soma(idProjeto, "TEMPPREVISTO");
		double somaTempoGasto = soma(idProjeto,"TEMPO");
		
		salvaTotalHoras(idProjeto,somaTempoPrevisto,"TEMPREVISTO");
		salvaTotalHoras(idProjeto,somaTempoGasto,"TEMPOGASTO");
	}

	private double soma(BigDecimal idProjeto, String campo) {
		double total = 0;

		try {

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_ETAPAPROJETO",
					"this.ID = ?", new Object[] { idProjeto}));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);

				BigDecimal tempo = (BigDecimal) DynamicVO.getProperty(campo);
				if(tempo==null) {
					tempo = new BigDecimal(0);
				}
				double x = tempo.doubleValue();

				total += x;

			}

		} catch (Exception e) {
			salvarException("[soma] nao foi possivel somar as horas! " + e.getMessage() + "\n" + e.getCause());
		}

		return total;
	}
	
	private void salvaTotalHoras(BigDecimal idProjeto, double horas, String campo) throws Exception {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_PROJETOS",
					"this.ID=? ", new Object[] { idProjeto }));
			
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty(campo, casasDecimais(2,new BigDecimal(horas)));

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			salvarException("[salvaTotalHoras] nao foi possivel salvar as horas! "+e.getMessage()+"\n"+e.getCause());
		}		

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

			VO.setProperty("OBJETO", "eventoCalculaTempoDasAtividades");
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
