package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class evento_novoEncerramentoOs implements EventoProgramavelJava{
	
	/**
	 * 17/10/2021 vs 1.1 reformulação do objeto.
	 */

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
	
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		
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
		verifica(arg0);
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		verifica(arg0);
	}
	
	private void verifica(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		Timestamp dtfechamentoSla = VO.asTimestamp("DHFECHAMENTOSLA");
		Timestamp dtfechamento = VO.asTimestamp("DTFECHAMENTO");
		Timestamp dtPausa = VO.asTimestamp("DHPAUSEAPP");
		BigDecimal horafinal = VO.asBigDecimal("HRFINAL");
		BigDecimal hrinicial = VO.asBigDecimal("HRINICIAL");
		Timestamp inicexec = VO.asTimestamp("INICEXEC");
		BigDecimal numitem = VO.asBigDecimal("NUMITEM");
		BigDecimal numos = VO.asBigDecimal("NUMOS");
		//String pendente = VO.asString("PENDENTE");
		String situacao = VO.asString("SITUACAO");
		String solucao = VO.asString("SOLUCAO");
		Timestamp termino = VO.asTimestamp("TERMEXEC");
		
		if("P".equals(situacao) && termino==null) {
			atualizaTCSITE(numitem,numos,inicexec,hrinicial);
		}
		
		if("F".equals(situacao) && termino!=null) {
			atualizaTCSITE(numitem,numos,hrinicial,horafinal,inicexec,termino,solucao,dtPausa);
			atualizaTCSOSE(numos,dtfechamento,dtfechamentoSla,situacao);
			VO.setProperty("PENDENTE", "N");
		}
	}
	
	private void atualizaTCSOSE(BigDecimal numos, Timestamp dtfechamento, Timestamp dtfechamentoSla, String situacao) {
		try {

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("OrdemServico",
					"this.NUMOS=? ", new Object[] { numos }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("DTFECHAMENTO", dtfechamento);
				VO.setProperty("DHFECHAMENTOSLA", dtfechamentoSla);
				VO.setProperty("SITUACAO", situacao);
				VO.setProperty("CODUSUFECH", getCodUsu(numos));
				VO.setProperty("CODCOS", new BigDecimal(4));

				itemEntity.setValueObject(NVO);
			}

		} catch (Exception e) {
			salvarException(
					"[atualizaTCSITE] nao foi possível atualizar a TCSITE! " + e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void atualizaTCSITE(BigDecimal numitem, BigDecimal numos, Timestamp inicexec, BigDecimal hrinicial) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("ItemOrdemServico",
					"this.NUMITEM=? AND this.NUMOS=? ", new Object[] { numitem, numos }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("INICEXEC", inicexec);
				VO.setProperty("HRINICIAL", hrinicial);

				itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			salvarException("[atualizaTCSITE] nao foi possível atualizar a TCSITE! "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void atualizaTCSITE(BigDecimal numitem, BigDecimal numos, BigDecimal hrinicial, BigDecimal horafinal, Timestamp inicexec, Timestamp termino, String solucao, Timestamp dtPausa) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("ItemOrdemServico",
					"this.NUMITEM=? AND this.NUMOS=? ", new Object[] { numitem, numos }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("HRINICIAL", hrinicial);
				VO.setProperty("HRFINAL", horafinal);
				VO.setProperty("INICEXEC", inicexec);
				VO.setProperty("TERMEXEC", termino);
				VO.setProperty("SOLUCAO", solucao);
				VO.setProperty("DHPAUSEAPP", dtPausa);
				VO.setProperty("CODSIT", new BigDecimal(4));

				itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			salvarException("[atualizaTCSITE] nao foi possível atualizar a TCSITE! "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private BigDecimal getCodUsu(BigDecimal numos) {
		BigDecimal codusu = null;
		try {
			JapeWrapper DAO = JapeFactory.dao("ItemOrdemServico");
			DynamicVO VO = DAO.findOne("NUMOS=? AND NUMITEM=?",new Object[] { numos, "1" });
			
			if(VO!=null) {
				codusu = VO.asBigDecimal("CODUSU");
			}

		} catch (Exception e) {
			// TODO: handle exception
		}
		
		if(codusu == null) {
			codusu = new BigDecimal(815);
		}
		
		return codusu;
	}
	
	/*
	private void start(PersistenceEvent arg0) {
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		
		String newPendente = newVO.asString("PENDENTE");
		Timestamp hora = newVO.asTimestamp("TERMEXEC");
		BigDecimal numos = newVO.asBigDecimal("NUMOS");
		
		try {
	
			if("S".equals(newPendente) && hora!=null) {
				
				newVO.setProperty("PENDENTE", "N");
				
				Timer timer = new Timer(10000, new ActionListener() {	
					@Override
					public void actionPerformed(ActionEvent e) {
						chamaPentaho();				
					}
				});
				timer.setRepeats(false);
				timer.start();
					
			}
		} catch (Exception e) {
			salvarException("[start] nao foi possivel chamar o pentaho! num os"+numos+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void chamaPentaho() {

		try {

			String site = (String) MGECoreParameter.getParameter("PENTAHOIP");
			String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC_New/Transformation/Sankhya-EncerramentoOS/";
			String objName = "J-Loop_visitas_encerradas";

			si.runJob(path, objName);

		} catch (Exception e) {
			e.getMessage();
		}
	}
	
	*/
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "evento_verificaEncerramentoOS");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
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
