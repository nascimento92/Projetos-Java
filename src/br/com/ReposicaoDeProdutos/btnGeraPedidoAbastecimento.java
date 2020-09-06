package br.com.ReposicaoDeProdutos;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;

import com.sankhya.util.BigDecimalUtil;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.BarramentoRegra;
import br.com.sankhya.modelcore.comercial.ComercialUtils;
import br.com.sankhya.modelcore.comercial.ConfirmacaoNotaHelper;
import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btnGeraPedidoAbastecimento implements AcaoRotinaJava {

	public String erro;
	private BigDecimal codUsu = BigDecimal.ZERO;
	private EntityFacade dwfFacade;
	private String id;
	@SuppressWarnings("rawtypes")
	private Map parans;
	private String mensagemSucesso;
	private String mensagemRetorno;
	private String tipoMovimento;
	private BigDecimal codContagemApp = new BigDecimal(0); /**/

	/**
	 * Gera o pedido de abestimento da tela reposição de produto.
	 * este foi modificado o original está no módulo 06
	 */
	public void doAction(ContextoAcao contexto) throws Exception {
		int qtdLinhas = contexto.getLinhas().length;

		if (qtdLinhas != 1) {
			contexto.mostraErro("Para gerar o pedido de abastecimento é preciso selecionar somente uma linha da tabela de Reposição! ");
		}

		Registro[] reg = contexto.getLinhas();
		BigDecimal codigo = (BigDecimal) reg[0].getCampo("CODIGO");
		codContagemApp = (BigDecimal) reg[0].getCampo("CODCONTAGEMAPP"); /**/

		String ajusteestoque = (String) reg[0].getCampo("AJUSTEESTOQUE");

		if (ajusteestoque == "") {
			ajusteestoque = "N";
		}

		if (ajusteestoque == "N") {
			contexto.mostraErro("Pedido só pode ser gerado após o ajuste de inventário!");
		}

		BigDecimal ReqSai = criaReq(contexto);

		PersistentLocalEntity persistentLocalEntity = dwfFacade
				.findEntityByPrimaryKey("TGFREPO", new Object[] { codigo });
		EntityVO AjO = persistentLocalEntity.getValueObject();
		DynamicVO AjustadoVO = (DynamicVO) AjO;

		String mensagem = " ";
		String msgsai = " ";
		String msgent = " ";

		@SuppressWarnings("unused")
		BigDecimal RetSai = null;

		if (ReqSai == null) {
			mensagem = "Não foi possivel gerar o Pedido de Abastecimento!";
			// contexto.mostraErro(mensagem);

		} else {
			Timestamp dataAtual = new Timestamp(System.currentTimeMillis());
			AjustadoVO.setProperty("NUNOTA", ReqSai);
			AjustadoVO.setProperty("DTGERACAO", dataAtual);
			persistentLocalEntity.setValueObject(AjO);
			mensagem = "Pedido Gerado! Nr. único:" + ReqSai.toString() + "%n%n";
		}

		if (ReqSai != null) {
			// String msg = "Deseja Confirmar o Pedido?";
			// boolean marca =
			// contexto.confirmarSimNao("Nr. Unico:"+ReqSai.toString(), msg, 1);
			// if (marca) {
			totalizaImpostos(ReqSai);
			// refazFinanceiro(ReqSai);
			// RetSai = confirmarNota(ReqSai,true);
			// msgsai = "Pedido Confirmado!"+"%n%n";
			// }

		}

		mensagem = String.format(mensagem + msgsai + msgent);

		contexto.setMensagemRetorno(mensagem);
	}

	public void setDwfFacade(EntityFacade dwfFacade) {
		this.dwfFacade = dwfFacade;
	}

	public void setId(String id) {
		this.id = id;
	}

	@SuppressWarnings("rawtypes")
	public void setParans(Map parans) {
		this.parans = parans;
	}

	public void setMensagemSucesso(String mensagemSucesso) {
		this.mensagemSucesso = mensagemSucesso;
	}

	public String getTipoMovimento() {
		return this.tipoMovimento;
	}

	private BigDecimal criaReq(final ContextoAcao contexto) throws Exception {

		BigDecimal nunota = null;
		BigDecimal nuNotaModelo = null;

		Registro[] Reg = contexto.getLinhas();

		codUsu = contexto.getUsuarioLogado();

		BigDecimal codemp = (BigDecimal) Reg[0].getCampo("CODEMP");
		BigDecimal codigo = (BigDecimal) Reg[0].getCampo("CODIGO");

		BigDecimal codcencus = (BigDecimal) Reg[0].getCampo("CODCENCUS");
		BigDecimal codnat = (BigDecimal) Reg[0].getCampo("CODNAT");
		BigDecimal codtipvenda = (BigDecimal) Reg[0].getCampo("CODTIPTIT");
		BigDecimal codtipoper = (BigDecimal) Reg[0].getCampo("CODTIPOPER");
		BigDecimal codparc = (BigDecimal) Reg[0].getCampo("CODPARC");
		BigDecimal codreg = (BigDecimal) Reg[0].getCampo("CODREG");
		BigDecimal codcontrato = (BigDecimal) Reg[0].getCampo("CODCONTRATO");
		BigDecimal codlocal = (BigDecimal) Reg[0].getCampo("CODLOCAL");

		dwfFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO Emp1VO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO(
				"EmpresaFinanceiro", new Object[] { codemp });
		nuNotaModelo = Emp1VO.asBigDecimal("AD_MODPEDABAST");

		if (nuNotaModelo == null
				|| nuNotaModelo.equals(new java.math.BigDecimal(0))) {
			Emp1VO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO(
					"EmpresaFinanceiro",
					new Object[] { new java.math.BigDecimal(1) });
			nuNotaModelo = Emp1VO.asBigDecimal("AD_MODPEDABAST");
		}

		if (nuNotaModelo == null
				|| nuNotaModelo.equals(new java.math.BigDecimal(0))) {
			contexto.mostraErro("E preciso informar, no cadastro da empresa um modelo valido para geracao do pedido de Abastecimento ");
		}

		QueryExecutor itens = contexto.getQuery();
		itens.setParam("CODIGO", codigo);
		itens.nativeSelect("SELECT * FROM AD_TGFREPOITE WHERE CODIGO={CODIGO} AND NVL(QTDPEDIDO,0)>0");
		while (itens.next()) {

			if (nunota == null) {
				nunota = criaCabecalho(nuNotaModelo);
				PersistentLocalEntity persistentLocalEntity = dwfFacade
						.findEntityByPrimaryKey("CabecalhoNota", nunota);
				EntityVO NVO = persistentLocalEntity.getValueObject();
				DynamicVO NotaGeradaVO = (DynamicVO) NVO;
				NotaGeradaVO.setProperty("CODCENCUS", codcencus);
				NotaGeradaVO.setProperty("CODEMP", codemp);
				NotaGeradaVO.setProperty("CODNAT", codnat);
				NotaGeradaVO.setProperty("CODTIPVENDA", codtipvenda);
				NotaGeradaVO.setProperty("CODTIPOPER", codtipoper);
				NotaGeradaVO.setProperty("CODPARC", codparc);
				NotaGeradaVO.setProperty("AD_CODREG", codreg);
				NotaGeradaVO.setProperty("NUMCONTRATO", codcontrato);
				NotaGeradaVO.setProperty("AD_CODLOCAL", codlocal);
				NotaGeradaVO.setProperty("CIF_FOB", "F");
				NotaGeradaVO.setProperty("CODUSU", codUsu);
				persistentLocalEntity.setValueObject(NVO);

			}

			criaItem(contexto, nunota, itens, codemp, codtipoper);

		}

		return nunota;
	}

	private BigDecimal criaCabecalho(BigDecimal nuModelo) throws Exception {
		BigDecimal nunota = new java.math.BigDecimal(0);
		try {

			BigDecimal nuNotaModelo = (BigDecimal) nuModelo;

			dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO padraoNPVO = dwfFacade
					.getDefaultValueObjectInstance(DynamicEntityNames.CABECALHO_NOTA);
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade
					.findEntityByPrimaryKeyAsVO("CabecalhoNota", nuNotaModelo);
			DynamicVO NotaProdVO = (DynamicVO) padraoNPVO;

			BigDecimal codTipOper = ModeloNPVO.asBigDecimal("CODTIPOPER");

			DynamicVO topRVO = ComercialUtils.getTipoOperacao(codTipOper);
			String tipoMovimento = topRVO.asString("TIPMOV");
			this.tipoMovimento = tipoMovimento;
			NotaProdVO.setProperty("CODTIPOPER", codTipOper);
			NotaProdVO.setProperty("TIPMOV", tipoMovimento);
			NotaProdVO.setProperty("SERIENOTA",
					ModeloNPVO.asString("SERIENOTA"));
			NotaProdVO.setProperty("CODPARC",
					ModeloNPVO.asBigDecimal("CODPARC"));
			NotaProdVO.setProperty("CODTIPVENDA",
					ModeloNPVO.asBigDecimal("CODTIPVENDA"));
			NotaProdVO.setProperty("CODEMP", ModeloNPVO.asBigDecimal("CODEMP"));
			NotaProdVO.setProperty("CODNAT", ModeloNPVO.asBigDecimal("CODNAT"));
			NotaProdVO.setProperty("CODCENCUS",
					ModeloNPVO.asBigDecimal("CODCENCUS"));
			NotaProdVO.setProperty("NUMNOTA", new java.math.BigDecimal(0));
			NotaProdVO.setProperty("APROVADO", ModeloNPVO.asString("APROVADO"));
			NotaProdVO.setProperty("PENDENTE", "S");
			NotaProdVO.setProperty("CIF_FOB", ModeloNPVO.asString("CIF_FOB"));

			dwfFacade.createEntity(DynamicEntityNames.CABECALHO_NOTA,
					(EntityVO) NotaProdVO);
			nunota = NotaProdVO.asBigDecimal("NUNOTA");

			//salva a nunota na tabela de inventario do app		
			
			if(codContagemApp!=null){
				if(codContagemApp.intValue()>0){ 
					registraPedidoGerado(codContagemApp,nunota);
				}
			}	
			
			return nunota;

		} catch (Exception e) {
			this.erro = e.getMessage();
			System.out.println("Problema ao criar cabecalho!!");
			e.printStackTrace();
		}
		return nunota;

	}

	private void criaItem(ContextoAcao contexto, BigDecimal nunota,
			QueryExecutor itens, BigDecimal codEmp, BigDecimal codtipoper)
			throws Exception {

		dwfFacade = EntityFacadeFactory.getDWFFacade();

		BigDecimal codProd = (BigDecimal) itens.getBigDecimal("CODPROD");
		DynamicVO produtoVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO(
				"Produto", new Object[] { codProd });
		String codVol = (String) produtoVO.asString("CODVOL");
		String usoProd = (String) produtoVO.asString("USOPROD");

		BigDecimal codlocal = new BigDecimal(1110);
		BigDecimal qtdneg = (BigDecimal) itens.getBigDecimal("QTDPEDIDO").abs();

		DynamicVO itemVO = (DynamicVO) dwfFacade
				.getDefaultValueObjectInstance(DynamicEntityNames.ITEM_NOTA);
		BigDecimal vlrCusSemICM = ComercialUtils.obtemPrecoCusto("S", " ",
				codEmp, codlocal, codProd);

		if (vlrCusSemICM == null) {
			vlrCusSemICM = new java.math.BigDecimal(0);
		}
		DynamicVO topRVO = ComercialUtils.getTipoOperacao(codtipoper);
		String topAtualEst = topRVO.asString("ATUALEST");
		String reserva = "N";
		if (topAtualEst.equals("R")) {
			reserva = "S";
		}
		BigDecimal VlrUnit = vlrCusSemICM;

		validaEstoque(contexto, codEmp, codlocal, codProd, " ", qtdneg);

		itemVO.setProperty("CODEMP", codEmp);
		itemVO.setProperty("CODPROD", codProd);
		itemVO.setProperty("USOPROD", usoProd);
		itemVO.setProperty("NUNOTA", nunota);
		itemVO.setProperty("VLRUNIT", vlrCusSemICM);
		itemVO.setProperty("QTDNEG", qtdneg);
		itemVO.setProperty("ATUALESTOQUE", atualEst(codtipoper));
		itemVO.setProperty("RESERVA", reserva);
		itemVO.setProperty("CODVOL", codVol);
		itemVO.setProperty("CODLOCALORIG", codlocal);
		itemVO.setProperty("VLRTOT",
				VlrUnit.multiply(qtdneg).setScale(3, BigDecimal.ROUND_HALF_UP));

		dwfFacade.createEntity(DynamicEntityNames.ITEM_NOTA, (EntityVO) itemVO);
	}

	public void totalizaImpostos(BigDecimal nunota) throws Exception {
		ImpostosHelpper impostos = new ImpostosHelpper();
		impostos.carregarNota(nunota);
		// impostos.forcaRecalculoBaseISS(true);
		impostos.setForcarRecalculo(true);
		// impostos.setForcarRecalculoIcmsZero(true);
		// impostos.setForcarRecalculoIpiZero(true);
		impostos.calcularImpostos(nunota);
		impostos.calcularTotalItens(nunota, true);
		impostos.calculaICMS(true);
		// impostos.calcularPIS();
		// impostos.calcularCSSL();
		// impostos.calcularCOFINS();
		impostos.totalizarNota(nunota);
		impostos.salvarNota();
	}

	private BigDecimal atualEst(BigDecimal codTipOper) throws Exception {

		DynamicVO topRVO = ComercialUtils.getTipoOperacao(codTipOper);
		String topAtualEst = topRVO.asString("ATUALEST");

		BigDecimal atualEst = new java.math.BigDecimal(0);

		if (topAtualEst.equals("E")) {
			atualEst = new java.math.BigDecimal(1);
		}
		if (topAtualEst.equals("B")) {
			atualEst = new java.math.BigDecimal(-1);
		}
		;
		if (topAtualEst.equals("N")) {
			atualEst = new java.math.BigDecimal(0);
		}
		;
		if (topAtualEst.equals("R")) {
			atualEst = new java.math.BigDecimal(1);
		}
		;
		return atualEst;

	}

	private void validaEstoque(ContextoAcao contexto, BigDecimal codemp,
			BigDecimal codlocal, BigDecimal codprod, String controle,
			BigDecimal qtdneg) throws Exception {

		ComercialUtils.ResultadoValidaEstoque ValidaEstoque = ComercialUtils
				.validaEstoque(codemp, codlocal, codprod, controle, "N");
		BigDecimal Estoque = ValidaEstoque.getQtdEst();
		if (Estoque.compareTo(qtdneg) == -1) {
			contexto.mostraErro("ATENÇÃO: Estoque Insuficiente! \r\n Produto:"
					+ codprod.toString() + "\r\nLocal:" + codlocal.toString()
					+ "\r\nEstoque:" + Estoque);
		}
	}

	public BigDecimal confirmarNota(BigDecimal NNota)throws MGEModelException {
		
		BigDecimal nuNota = BigDecimalUtil.getValueOrZero(NNota);
		
		try {
			if (!nuNota.equals(new java.math.BigDecimal(0))) {

				ServiceContext serviceCtx = ServiceContext.getCurrent();
				JapeSession.putProperty("CabecalhoNota.confirmacao.ehPedido.Web",Boolean.FALSE);
				AuthenticationInfo auth = (AuthenticationInfo) serviceCtx.getAutentication();
				BarramentoRegra bRegras = BarramentoRegra.build(CACHelper.class, "regrasAprovarCAC.xml", auth);

				CACHelper.setupContext(serviceCtx);

				ConfirmacaoNotaHelper.confirmarNota(nuNota, bRegras);

			}
		} catch (Exception e) {
			MGEModelException.throwMe(e);
			nuNota = new java.math.BigDecimal(0);
		} 
		
		return nuNota;
	}
	
	//salva o pedido gerado na tabela do app
	public void registraPedidoGerado(BigDecimal codContagemApp, BigDecimal nuNota) throws Exception{ /**/

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			PersistentLocalEntity persistentLocalEntity = dwfFacade.findEntityByPrimaryKey("ContagemEstoqueAvancado", codContagemApp);
			EntityVO NVO = persistentLocalEntity.getValueObject();
			DynamicVO appVO = (DynamicVO) NVO;
			
			appVO.setProperty("NUNOTA", nuNota);
			
			persistentLocalEntity.setValueObject(NVO);
	
	}

}

