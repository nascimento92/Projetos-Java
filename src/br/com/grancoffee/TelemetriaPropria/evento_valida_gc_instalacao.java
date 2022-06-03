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
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class evento_valida_gc_instalacao implements EventoProgramavelJava{
	
	/**
	 * 27/05/2022 vs 1.4 Inserida no before insert para já marcar a máquina como liberada.
	 * 03/06/2022 vs 1.5 Inserido o método para as validações gerais
	 */

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		String patrimonio = VO.asString("CODBEM");
		cadastraTelemetrias(new BigDecimal(1), patrimonio);
		cadastraTelemetrias(new BigDecimal(2), patrimonio);
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
			
			if("S".equals(loja)) {
				
				if(!verificaGrupoProdutoDaMaquina(patrimonio)) {
					throw new Error("<br/><b>ATENÇÃO</b><br/>Patrimônio não pode ser marcado como <b>Micro Market</b>.<br/><br/><b>motivo:</b> No cadastro do grupo de produtos deste patrimônio o campo Loja não está tickado!<br/><br/>");
				}
				
				if(verificaTeclasDuplicadas(patrimonio)) {
					throw new Error("<br/><b>ATENÇÃO</b><br/>O patrimônio possuí teclas com produtos repetidos, não é possível transforma-lo em uma loja! Ajuste o planograma.<br/><br/>");
				}
			}
			
			if(verificaVisitaPendente(patrimonio)) {
				throw new Error("<br/><b>ATENÇÃO</b><br/>O patrimônio possuí visitas pendentes! não é possível alterar de loja para máquina ou vice e versa.<br/><br/>");
			}else {
				
				//TODO::Excluir teclas 
				excluirTeclas(patrimonio);
				verificaTeclasContrato(patrimonio, loja);
			}
		}
		
		ajustesGerais(arg0);
	}
	
	//Valiações
	private boolean verificaTeclasDuplicadas(String patrimonio) {
		boolean valida = false;
		
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT CODPROD, COUNT(*) AS QTD FROM AD_TECLAS WHERE CODBEM='"+patrimonio+"' GROUP BY CODPROD HAVING COUNT(*)>1");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("QTD");
				if (count > 1) {
					valida = true;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return valida;
	}
	
	private void ajustesGerais(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		String frequencia = VO.asString("AD_FREQCONTAGEM");
		String dia = VO.asString("AD_DIAINVENTARIO");
		
		if("1".equals(frequencia)) {
			VO.setProperty("AD_DIAINVENTARIO", null);
		}else {
			if(frequencia!=null && dia==null) {
				VO.setProperty("AD_DIAINVENTARIO", "2");
			}
		}
	}
	
	private boolean verificaVisitaPendente(String patrimonio) {
		boolean valida = false;

		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT COUNT(*) AS QTD FROM GC_SOLICITABAST WHERE CODBEM='"+patrimonio+"' AND STATUS='1'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("QTD");
				if (count >= 1) {
					valida = true;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		return valida;
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
		
		if(verificaGrupoProdutoDaMaquina(patrimonio)) {
			VO.setProperty("TOTEM", "S");
		}
		
		String loja = VO.asString("TOTEM");
		
		VO.setProperty("AD_LIBERADA", "S");
		VO.setProperty("AD_DTLIBERADA", TimeUtils.getNow());;
		
		if("S".equals(loja)) {
			if(!verificaGrupoProdutoDaMaquina(patrimonio)) {
				throw new Error("<br/><b>ATENÇÃO</b><br/>Patrimônio não pode ser marcado como <b>Micro Market</b>.<br/><br/><b>motivo:</b> No cadastro do grupo de produtos deste patrimônio o campo Loja não está tickado!<br/><br/>");
			}
		}
		
		ajustesGerais(arg0);
	}
	
	private void cadastraTelemetrias(BigDecimal idTelemetria, String codbem) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCTelemInstalacao");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("AD_INTEGRADO", "N");
			VO.setProperty("CODBEM", codbem);
			VO.setProperty("IDTEL", idTelemetria);
			
			dwfFacade.createEntity("GCTelemInstalacao", (EntityVO) VO);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	private boolean verificaGrupoProdutoDaMaquina(String patrimonio) {
		boolean valida = true;
		
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT NVL(G.AD_LOJA,'N') AS LOJA FROM AD_PATRIMONIO A JOIN TGFPRO P ON (P.CODPROD=A.CODPROD) JOIN TGFGRU G ON (G.CODGRUPOPROD=P.CODGRUPOPROD) WHERE A.CODBEM='"+patrimonio+"'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				String loja = contagem.getString("LOJA");
				if("N".equals(loja)) {
					valida = false;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return valida;
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
