package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class evento_valida_gc_instalacao implements EventoProgramavelJava{

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
		insert(arg0);		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		update(arg0);		
	}
	
	private void update(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		
		String abastecimento = VO.asString("ABASTECIMENTO");
		String oldAbastecimento = oldVO.asString("ABASTECIMENTO");
		
		String patrimonio = VO.asString("CODBEM");
		String valid = "";
		
		if(abastecimento!=oldAbastecimento) {
			
			if("S".equals(abastecimento)) {
				valid = "S";
			}else {
				valid = "N";
			}
			
			VO.setProperty("AD_NOPICK", valid);
			registraFila(patrimonio,valid);
		}
		
		String loja = VO.asString("TOTEM");
		String oldLoja = oldVO.asString("TOTEM");
		
		if(loja!=oldLoja) {
						
			//TODO::Excluir teclas 
			excluirTeclas(patrimonio);
			verificaTeclasContrato(patrimonio, loja);
			
			//TODO::Atualizar teclas na nota e atualiza troca de grade
			getVisitasPendentes(patrimonio, loja);
			
		}
	}
	
	//ALTERAR MÁQUINA P/LOJA
	//1° exclui as teclas
	private void excluirTeclas(String codbem) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("GCPlanograma", "this.CODBEM=?",new Object[] {codbem}));
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	//2° Pega a lista de teclas do contrato
	private void verificaTeclasContrato(String codbem, String loja) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("teclas", "this.CODBEM = ? ", new Object[] { codbem }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				
				BigDecimal tecla = null;
				
				if("S".equals(loja)) {
					tecla = new BigDecimal(0);
				}else {
					tecla = DynamicVO.asBigDecimal("TECLA");
				}
				
				BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
				BigDecimal nivelpar = DynamicVO.asBigDecimal("AD_NIVELPAR");
				BigDecimal capacidade = DynamicVO.asBigDecimal("AD_CAPACIDADE");
				BigDecimal nivelalerta = DynamicVO.asBigDecimal("AD_NIVELALERTA");
				BigDecimal vlrpar = DynamicVO.asBigDecimal("VLRPAR");
				BigDecimal vlrfun = DynamicVO.asBigDecimal("VLRFUN");
				BigDecimal estoque = getEstoque(codbem,produto);
				
				insereTecla(codbem, tecla.toString(), produto, nivelpar,capacidade, nivelalerta, vlrpar, vlrfun, estoque);
				 

			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	//3° Cadastra as teclas na gc_planograma
	private void insereTecla(String codbem, String tecla, BigDecimal produto, BigDecimal nivelpar, BigDecimal capacidade, 
			BigDecimal nivelalerta, BigDecimal vlrpar, BigDecimal vlrfun, BigDecimal estoque) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCPlanograma");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODBEM", codbem);
			VO.setProperty("TECLA", tecla);
			VO.setProperty("CODPROD", produto);
			VO.setProperty("NIVELPAR", nivelpar);
			VO.setProperty("CAPACIDADE", capacidade);
			VO.setProperty("NIVELALERTA", nivelalerta);
			VO.setProperty("VLRPAR", vlrpar);
			VO.setProperty("VLRFUN", vlrfun);
			VO.setProperty("ESTOQUE", estoque);
			VO.setProperty("AD_ABASTECER", "S");
			VO.setProperty("AD_VEND30D", new BigDecimal(0));
			
			dwfFacade.createEntity("GCPlanograma", (EntityVO) VO);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	//4° Atualiza as teclas da nota
	private void getVisitasPendentes(String patrimonio, String loja) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("GCSolicitacoesAbastecimento", "this.CODBEM = ? AND this.STATUS=?", new Object[] { patrimonio, "1"}));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);

				BigDecimal os = DynamicVO.asBigDecimal("NUMOS");
				BigDecimal nunota = DynamicVO.asBigDecimal("NUNOTA");
				
				if(nunota!=null) { //atualiza teclas na nota
					verificaTeclasContrato2(patrimonio,loja,nunota);
				}
				
				excluirTrocaGrade(patrimonio,os);
				//TODO :: EXCLUIR PLANOGRAMA PENDENTE
				
				//TODO :: CADASTRAR NA PLANOGRAMA PENDENTE E NA TROCA DE GRADE
				verificaTeclasContrato3(patrimonio,loja,os, nunota);
			}

		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	//5° Verifica as teclas novamente para atualizar na nota
	private void verificaTeclasContrato2(String codbem, String loja, BigDecimal nunota) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("teclas", "this.CODBEM = ? ", new Object[] { codbem }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				
				BigDecimal tecla = null;
				
				if("S".equals(loja)) {
					tecla = new BigDecimal(0);
				}else {
					tecla = DynamicVO.asBigDecimal("TECLA");
				}
				
				BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
				
				atualizaTeclaNaNota(nunota, produto, tecla.toString());

			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	//6° Atualiza tecla na nota
	private void atualizaTeclaNaNota(BigDecimal nunota, BigDecimal produto, String tecla) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("ItemNota",
					"this.NUNOTA=? AND this.CODPROD=? ", new Object[] { nunota, produto }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("AD_TECLA", tecla);

				itemEntity.setValueObject(NVO);
			}

		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	//7° Exclui ad_trocadegrade
	private void excluirTrocaGrade(String codbem, BigDecimal numos) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_TROCADEGRADE", "this.CODBEM=? AND this.NUMOS=?",new Object[] {codbem, numos}));
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	//8° Verifica teclas para serem inseridas na troca de grade
	private void verificaTeclasContrato3(String codbem, String loja, BigDecimal numos, BigDecimal nunota) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("teclas", "this.CODBEM = ? ", new Object[] { codbem }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				
				BigDecimal tecla = null;
				
				if("S".equals(loja)) {
					tecla = new BigDecimal(0);
				}else {
					tecla = DynamicVO.asBigDecimal("TECLA");
				}
				
				BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
				BigDecimal nivelpar = DynamicVO.asBigDecimal("AD_NIVELPAR");
				BigDecimal capacidade = DynamicVO.asBigDecimal("AD_CAPACIDADE");
				BigDecimal nivelalerta = DynamicVO.asBigDecimal("AD_NIVELALERTA");
				BigDecimal vlrpar = DynamicVO.asBigDecimal("VLRPAR");
				BigDecimal vlrfun = DynamicVO.asBigDecimal("VLRFUN");
				BigDecimal vlrfinal = vlrpar.add(vlrfun);
				
				BigDecimal qtdabast = null;
				if (nunota != null) {
					qtdabast = validaQuantidadeParaAbastecer(nunota, tecla.toString(), produto);
				} else {
					qtdabast = new BigDecimal(0);
				}
				
				insertAD_TROCADEGRADE(codbem,numos,produto,tecla.toString(),vlrfinal, capacidade,nivelpar,qtdabast,"","",nivelalerta,vlrpar,vlrfun);

			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	private BigDecimal validaQuantidadeParaAbastecer(BigDecimal nunota, String tecla, BigDecimal produto) {
		BigDecimal qtdabast = BigDecimal.ZERO;
		
		try {
			JapeWrapper DAO = JapeFactory.dao("ItemNota");
			DynamicVO VO = DAO.findOne("NUNOTA=? and CODPROD=? and AD_TECLA=?",new Object[] { nunota, produto, tecla });
			
			if(VO!=null) {
				qtdabast = VO.asBigDecimal("QTDNEG");
			}

		} catch (Exception e) {

		}
		
		return qtdabast;
	}
	
	//9° Insere na troca de grade
	private void insertAD_TROCADEGRADE(String patrimonio, BigDecimal numos, BigDecimal produto, String tecla, BigDecimal valorFinal, BigDecimal capacidade, BigDecimal nivelpar, 
			BigDecimal qtdabast, String statuspar, String statusvalor, BigDecimal nivelalerta, BigDecimal vlrpar, BigDecimal vlrfun) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_TROCADEGRADE");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("NUMOS", numos);
			VO.setProperty("CODPROD", produto);
			VO.setProperty("TECLA", tecla);
			VO.setProperty("VALOR", valorFinal);
			VO.setProperty("CAPACIDADE", capacidade);
			VO.setProperty("NIVELPAR", nivelpar);
			VO.setProperty("QTDABAST", qtdabast);
			VO.setProperty("QTDCONTAGEM", new BigDecimal(0));
			VO.setProperty("QTDRET", new BigDecimal(0));
			VO.setProperty("MOLAVAZIA", "N");
			VO.setProperty("STATUS_PAR", statuspar);
			VO.setProperty("STATUS_VLR", statusvalor);
			VO.setProperty("NIVELALERTA", nivelalerta);
			VO.setProperty("VLRPAR", vlrpar);
			VO.setProperty("VLRFUN", vlrfun);
			VO.setProperty("QTDRET", new BigDecimal(0));
			
			dwfFacade.createEntity("AD_TROCADEGRADE", (EntityVO) VO);
			
		} catch (Exception e) {
			 
		}
	}
	
	//10° Atualiza troca de grade
	private void validaItensDaTrocaDeGrade(String patrimonio, BigDecimal numos) {
		
		//TODO :: verificar itens para retirar
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_PLANOGRAMAATUAL","this.CODBEM = ? ", new Object[] { patrimonio }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			String tecla = DynamicVO.asString("TECLA");
			BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
			BigDecimal vlrpar = DynamicVO.asBigDecimal("VLRPAR");
			BigDecimal vlrfun = DynamicVO.asBigDecimal("VLRFUN");
			BigDecimal valorFinal = vlrpar.add(vlrfun);	
			BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
			BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
			BigDecimal nivelalerta = DynamicVO.asBigDecimal("NIVELALERTA");
			
			boolean existeNoPlanogramaPendente = validaSeExisteNaPlanogramaPendenteOuAtual(patrimonio,produto,tecla,numos, "AD_PLANOGRAMAPENDENTE");
			
			if(!existeNoPlanogramaPendente) {
				//TODO :: inserir para retirar
				insertAD_TROCADEGRADE(patrimonio, numos, produto, tecla, valorFinal, capacidade, nivelpar, new BigDecimal(0), "RETIRAR", "RETIRAR", nivelalerta, vlrpar, vlrfun);
				
				//TODO :: Para visita de secos, deve ser retirado apenas os secos e para congelados os congelados.
				//?? pendente
			}

			}
		} catch (Exception e) {
			salvarException("[validaItensDaTrocaDeGrade] Nao foi possivel verificar os produtos a serem retirados " + patrimonio
					+ e.getMessage() + "\n" + e.getCause());
		}
		
		//TODO :: verificar itens novos e já existentes
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_PLANOGRAMAPENDENTE","this.NUMOS = ? ", new Object[] { numos }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			String tecla = DynamicVO.asString("TECLA");
			BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
			BigDecimal vlrpar = DynamicVO.asBigDecimal("VLRPAR");
			BigDecimal vlrfun = DynamicVO.asBigDecimal("VLRFUN");
			BigDecimal valorFinal = vlrpar.add(vlrfun);	
			BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
			
			boolean existeNaPlanogramaAtual = validaSeExisteNaPlanogramaPendenteOuAtual(patrimonio,produto,tecla,null,"AD_PLANOGRAMAATUAL");
			
			if(!existeNaPlanogramaAtual) { // produto novo
				atualizaStatusPlanogramaPendente(numos,produto,tecla,patrimonio,"NOVO", "NOVO");
			}else { //produto já existe
				DynamicVO planogramaAtual = getPlanogramaAtual(patrimonio, produto, tecla);
				BigDecimal parAtual = planogramaAtual.asBigDecimal("NIVELPAR");
				BigDecimal vlrParAtual = planogramaAtual.asBigDecimal("VLRPAR");
				BigDecimal vlrFunAtual = planogramaAtual.asBigDecimal("VLRFUN");
				BigDecimal valorFinalAtual = vlrParAtual.add(vlrFunAtual);
				
				String statusPar = "";
				String statusVlr = "";
				
				if(nivelpar.intValue()>parAtual.intValue()) {
					statusPar = "AUMENTO PAR";
				}else if (nivelpar.intValue()<parAtual.intValue()) {
					statusPar = "REDUCAO PAR";
				}else {
					statusPar = "IGUAL";
				}
				
				if(valorFinal.doubleValue()>valorFinalAtual.doubleValue()) {
					statusVlr = "AUMENTO VLR";
				}else if (valorFinal.doubleValue()<valorFinalAtual.doubleValue()) {
					statusVlr = "REDUCAO VLR";
				}else {
					statusVlr = "IGUAL";
				}
				
				atualizaStatusPlanogramaPendente(numos,produto,tecla,patrimonio,statusPar, statusVlr);
				
			}

			}
		} catch (Exception e) {
			salvarException("[validaItensDaTrocaDeGrade] Nao foi possivel verificar os produtos novos e existes " + patrimonio
					+ e.getMessage() + "\n" + e.getCause());
		}

	}
	
	
	//OUTROS PROCESSOS
	
	private void insert(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		String abastecimento = VO.asString("ABASTECIMENTO");
		String patrimonio = VO.asString("CODBEM");
		String valid = "";
		
		if("S".equals(abastecimento)) {
			valid = "S";	
		}else {
			valid = "N";
		}
	
		VO.setProperty("AD_NOPICK", valid);
		registraFila(patrimonio,valid);
	}
	
	private BigDecimal getEstoque(String patrimonio, BigDecimal produto) {
		BigDecimal valor = BigDecimal.ZERO;
		
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"select ESTOQUE from ad_estoque where codbem='"+patrimonio+"' and codprod="+produto+" and estoque>0 and rownum=1");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				BigDecimal bigDecimal = contagem.getBigDecimal("ESTOQUE");
				
				if(bigDecimal!=null) {
					valor = bigDecimal;
				}
				
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return valor;
	}
	
	private void registraFila(String patrimonio, String nopick) {
	
		BigDecimal usu = BigDecimalUtil.getValueOrZero(((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_INTTP");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("TIPO", "P");
			VO.setProperty("IGNORESYNC", nopick);
			VO.setProperty("DTSOLICIT", TimeUtils.getNow());
			VO.setProperty("CODUSU", usu);
			VO.setProperty("CODBEM", patrimonio);
			
			dwfFacade.createEntity("AD_INTTP", (EntityVO) VO);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}
