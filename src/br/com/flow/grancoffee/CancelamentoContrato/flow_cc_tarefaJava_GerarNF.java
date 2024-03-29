package br.com.flow.grancoffee.CancelamentoContrato;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.ComercialUtils;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class flow_cc_tarefaJava_GerarNF implements TarefaJava {
	
	String patrimonios="";
	
	@Override
	public void executar(ContextoTarefa arg0) throws Exception {
		start(arg0);
	}

	private void start(ContextoTarefa arg0) {
		Object idflow = arg0.getIdInstanceProcesso();
		verificaPlantas(idflow, arg0);
	}

	private void verificaPlantas(Object idflow, ContextoTarefa arg0) {
		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT DISTINCT idplanta FROM AD_PATCANCELAMENTO WHERE IDINSTPRN=" + idflow);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				BigDecimal planta = contagem.getBigDecimal("idplanta");
				if (planta != null) {
					criarNfParaAhPlanta(idflow, planta, arg0);
				}
			}

		} catch (Exception e) {			
			salvarException("[verificaPlantas] N�o foio possivel determinar as plantas! "+e.getMessage()+"\n"+e.getCause());
		}
	}

	private void criarNfParaAhPlanta(Object idflow, BigDecimal planta, ContextoTarefa arg0) throws Exception {
		// Object usuarioInclusao = arg0.getCampo("SYS_USUARIOINCLUSAO");
		BigDecimal nunota = criaCabecalho(idflow,planta);
		if (nunota != null) {
			insereNotaRetorno(nunota, idflow);
			getPatrimonios(idflow, planta, nunota);
			totalizaImpostos(nunota);
		}
		// insere cada um dos pt na tgfite da nota criada.

	}

	public BigDecimal criaCabecalho(Object idflow,BigDecimal planta) throws Exception {

		DynamicVO form = getForm(idflow);
		BigDecimal centroResultado = form.asBigDecimal("CODCENCUS");
		BigDecimal topRetorno = getTopRetorno(centroResultado);
		BigDecimal nuNotaModelo = BigDecimal.ZERO;
		BigDecimal nunota = BigDecimal.ZERO;
		String tipoRetirada = "";

		if (topRetorno.intValue() == 0) {

		} else {
			nuNotaModelo = getNotaModelo(topRetorno);
		}
		
		String patrimonios = "PATRIM�NIOS: \n";
		geraDescricaoDosPatrimonios(idflow,planta);

		if ("1".equals(form.asString("TIPOCANCEL"))) {
			tipoRetirada = "CANCELAMENTO PARCIAL \n"+patrimonios+this.patrimonios;
		} else {
			tipoRetirada = "CANCELAMENTO TOTAL \n"+patrimonios+this.patrimonios;
		}

		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO padraoNPVO = dwfFacade.getDefaultValueObjectInstance(DynamicEntityNames.CABECALHO_NOTA);
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("CabecalhoNota", nuNotaModelo);
			DynamicVO NotaProdVO = (DynamicVO) padraoNPVO;

			BigDecimal codTipOper = ModeloNPVO.asBigDecimal("CODTIPOPER");

			DynamicVO topRVO = ComercialUtils.getTipoOperacao(codTipOper);
			String tipoMovimento = topRVO.asString("TIPMOV");

			NotaProdVO.setProperty("CODTIPOPER", codTipOper);
			NotaProdVO.setProperty("TIPMOV", tipoMovimento);
			NotaProdVO.setProperty("SERIENOTA", ModeloNPVO.asString("SERIENOTA"));
			NotaProdVO.setProperty("CODPARC", form.asBigDecimal("CODPARC"));
			NotaProdVO.setProperty("NUMCONTRATO", form.asBigDecimal("NUMCONTRATO"));
			NotaProdVO.setProperty("CODTIPVENDA", ModeloNPVO.asBigDecimal("CODTIPVENDA"));
			NotaProdVO.setProperty("CODEMP", getTCSCON(form.asBigDecimal("NUMCONTRATO")).asBigDecimal("CODEMP"));
			NotaProdVO.setProperty("CODNAT", ModeloNPVO.asBigDecimal("CODNAT"));
			NotaProdVO.setProperty("CODCENCUS", ModeloNPVO.asBigDecimal("CODCENCUS"));
			NotaProdVO.setProperty("NUMNOTA", new java.math.BigDecimal(0));
			NotaProdVO.setProperty("APROVADO", ModeloNPVO.asString("APROVADO"));
			NotaProdVO.setProperty("PENDENTE", "S");
			NotaProdVO.setProperty("CIF_FOB", ModeloNPVO.asString("CIF_FOB"));
			NotaProdVO.setProperty("CODEMPNEGOC", ModeloNPVO.asBigDecimal("CODEMP"));
			NotaProdVO.setProperty("TIPFRETE", "N");
			NotaProdVO.setProperty("OBSERVACAO", tipoRetirada);

			dwfFacade.createEntity(DynamicEntityNames.CABECALHO_NOTA, (EntityVO) NotaProdVO);
			nunota = NotaProdVO.asBigDecimal("NUNOTA");

			return nunota;

		} catch (Exception e) {
			salvarException("[criaCabecalho] Nao foi possivel gerar cabecalho! "+e.getMessage()+"\n"+e.getCause());
		}
		return nunota;
	}

	private DynamicVO getForm(Object idflow) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_FORMCANCELAMENTO");
		DynamicVO VO = DAO.findOne("IDINSTPRN=?", new Object[] { idflow });
		return VO;
	}

	private BigDecimal getTopRetorno(BigDecimal centro) throws Exception {
		BigDecimal top = BigDecimal.ZERO;
		JapeWrapper DAO = JapeFactory.dao("CentroResultado");
		DynamicVO VO = DAO.findOne("CODCENCUS=?", new Object[] { centro });
		if (VO != null) {
			BigDecimal topRetorno = VO.asBigDecimal("AD_TOPCANCELAMENTO");
			if (topRetorno != null) {
				top = topRetorno;
			}
		}

		return top;
	}

	private BigDecimal getNotaModelo(BigDecimal top) {
		BigDecimal nunota = BigDecimal.ZERO;
		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT MIN(NUNOTA) AS NUNOTA FROM TGFCAB WHERE CODTIPOPER=" + top
					+ " AND DTNEG>=SYSDATE-30 AND PENDENTE='N' AND STATUSNOTA = 'L'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				BigDecimal count = contagem.getBigDecimal("NUNOTA");
				if (count != null) {
					nunota = count;
				}
			}

		} catch (Exception e) {
			salvarException("[getNotaModelo] Nao foi possivel obter a nota modelo! "+e.getMessage()+"\n"+e.getCause());
		}

		return nunota;
	}

	private DynamicVO getTCSCON(BigDecimal contrato) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Contrato");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=?", new Object[] { contrato });
		return VO;
	}

	private void insereNotaRetorno(BigDecimal nunota, Object idflow) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_NFCANCELAMENTO");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("CODREGISTRO", new BigDecimal(1));
			VO.setProperty("IDINSTPRN", idflow);
			VO.setProperty("IDINSTTAR", new BigDecimal(0));
			VO.setProperty("IDTAREFA", "UserTask_13orzyu");
			VO.setProperty("NUNOTA", nunota);

			dwfFacade.createEntity("AD_NFCANCELAMENTO", (EntityVO) VO);

		} catch (Exception e) {
			salvarException("[insereNotaRetorno] Nao foi possivel salvar as NF de retorno! "+e.getMessage()+"\n"+e.getCause());
		}
	}

	private void getPatrimonios(Object idflow, BigDecimal planta, BigDecimal nunota) {
		int cont = 1;

		try {

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_PATCANCELAMENTO",
					"this.IDINSTPRN = ? and this.IDPLANTA=? ", new Object[] { idflow, planta }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);

				inserePatrimonioNaNota(DynamicVO, nunota, cont);
				cont++;
			}

		} catch (Exception e) {
			salvarException("[getPatrimonios] N�o foi possivel obter os patrimonios! "+e.getMessage()+"\n"+e.getCause());
		}
	}

	private void inserePatrimonioNaNota(DynamicVO DynamicVO, BigDecimal nunota, int sequencia) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("ItemNota");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("CONTROLE", DynamicVO.asString("CODBEM"));
			VO.setProperty("CODPROD", DynamicVO.asBigDecimal("CODPROD"));
			VO.setProperty("NUNOTA", nunota);
			VO.setProperty("SEQUENCIA", new BigDecimal(sequencia));
			VO.setProperty("CODEMP", getTgfCab(nunota).asBigDecimal("CODEMP"));
			VO.setProperty("QTDNEG", new BigDecimal(1));
			
			if(getPrecoMaquina(DynamicVO.asString("CODBEM")).intValue()>0) {
				VO.setProperty("VLRUNIT", getPrecoMaquina(DynamicVO.asString("CODBEM")));
				VO.setProperty("VLRTOT", getPrecoMaquina(DynamicVO.asString("CODBEM")));
			}else {
				VO.setProperty("VLRUNIT", new BigDecimal(0));
				VO.setProperty("VLRTOT", new BigDecimal(0));
			}

			VO.setProperty("ATUALESTOQUE", new BigDecimal(1));
			VO.setProperty("CODVOL", getTgfPro(DynamicVO.asBigDecimal("CODPROD")).asString("CODVOL"));
			VO.setProperty("CODLOCALORIG", new BigDecimal(1500));

			dwfFacade.createEntity("ItemNota", (EntityVO) VO);
			
			atualizaNotaRetornoNaTciBem(DynamicVO.asString("CODBEM"),nunota);

		} catch (Exception e) {			
			salvarException("[inserePatrimonioNaNota] N�o foi possivel salvar os patrimonios na nota! "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private DynamicVO getTgfPro(BigDecimal codprod) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?", new Object[] { codprod });
		return VO;
	}

	private DynamicVO getTgfCab(BigDecimal nunota) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("CabecalhoNota");
		DynamicVO VO = DAO.findOne("NUNOTA=?", new Object[] { nunota });
		return VO;
	}

	private BigDecimal getPrecoMaquina(String patrimonio) {
		BigDecimal preco = BigDecimal.ZERO;

		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
			"SELECT VLR FROM FLOW_CANCELAMENTO_VLRMAQUINAS WHERE CODBEM='"+patrimonio+"'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				BigDecimal count = contagem.getBigDecimal("VLR");
				if(count!=null) {
					preco=count;
				}
			}

		} catch (Exception e) {
			salvarException("[getPrecoMaquina] N�o foi possivel obter o preco da maquina! "+e.getMessage()+"\n"+e.getCause());
			
		}

		return preco;
	}
	
	private void atualizaNotaRetornoNaTciBem(String patrimonio, BigDecimal notaRetorno) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("Imobilizado",
					"this.CODBEM=?", new Object[] { patrimonio }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("NUNOTADEV", notaRetorno);

				itemEntity.setValueObject(NVO);
			}

		} catch (Exception e) {
			salvarException("[atualizaNotaRetornoNaTciBem] N�o foi possivel salvar a nota de retorno na tcibem! "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	public void totalizaImpostos(BigDecimal nunota) throws Exception{
        ImpostosHelpper impostos = new ImpostosHelpper();
        impostos.carregarNota(nunota);
        impostos.setForcarRecalculo(true);
        impostos.calcularImpostos(nunota);
        impostos.calcularTotalItens(nunota, true);
        impostos.calculaICMS(true);
        impostos.totalizarNota(nunota);
        impostos.salvarNota();
	}
	
	private void geraDescricaoDosPatrimonios(Object idflow, BigDecimal planta) {
		try {

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_PATCANCELAMENTO",
					"this.IDINSTPRN = ? and this.IDPLANTA=? ", new Object[] { idflow, planta }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);
				
				this.patrimonios+=DynamicVO.asString("CODBEM")+", ";
			}

		} catch (Exception e) {
			salvarException("[geraDescricaoDosPatrimonios] Nao foi possivel obter a descricao dos patrimonios! "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void salvarException(String mensagem) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "flow_cc_tarefaJava_GerarNF");
			VO.setProperty("PACOTE", "br.com.flow.grancoffee.CancelamentoContrato");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
		} catch (Exception e) {
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}
}
