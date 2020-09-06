package br.com.PedidosImportados;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.comercial.ComercialUtils;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
	/**
	 * 
	 * @author gabriel.nascimento
	 * 
	 * - Objeto para criar os pedidos dos dados importados da planilha de pedidos.
	 *
	 */
public class btnGerarPedidos implements AcaoRotinaJava {

	private EntityFacade dwfFacade;
	private BigDecimal codUsu = BigDecimal.ZERO;
	public String erro;
	public Integer result = 0;
	public Date dataTop = null;
	public Date dataTipNeg = null;

	public void doAction(ContextoAcao contexto) throws Exception {
		// TODO Auto-generated method stub
		BigDecimal nunota = null;
		BigDecimal id = BigDecimal.ZERO;

		QueryExecutor sql = contexto.getQuery();

		sql.nativeSelect(
				"SELECT * FROM AD_PEDIDOSIMPORTADOS WHERE NVL(PEDIDOGERADO,'N')='N' AND NVL(NUNOTA,0)=0 ORDER BY IDPEDIDO");

		while (sql.next()) {

			BigDecimal idtabela = sql.getBigDecimal("IDPEDIDO");
			BigDecimal idLinha = sql.getBigDecimal("ID");

			
			if (idtabela.intValue() != id.intValue()) {
				nunota = null;
			}

			nunota = criaReq(contexto, idLinha, nunota);

			inserirItem(nunota, contexto, idLinha);

			salvarRetorno(idLinha, nunota);

			id = idtabela;
			

		}

		sql.nativeSelect(
				"SELECT COUNT(*) AS QTD FROM AD_PEDIDOSIMPORTADOS WHERE NVL(PEDIDOGERADO,'N')='N' AND NVL(NUNOTA,0)=0");

		while (sql.next()) {

			this.result = sql.getInt("QTD");

			if (result == 0) {
				contexto.setMensagemRetorno("Não existem mais pedidos pendentes para serem gerados!");
			}
		}

	}

	private BigDecimal criaReq(final ContextoAcao contexto, BigDecimal id, BigDecimal nunota)throws Exception {

		// descobrindo de acordo com a top que foi usada, qual modelo de nota eu preciso pegar
		dwfFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO pedidosImportadosVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("AD_PEDIDOSIMPORTADOS",
				new Object[] { id });

		BigDecimal codipoper = (BigDecimal) pedidosImportadosVO.getProperty("CODTIPOPER");
		BigDecimal codtipvenda = (BigDecimal) pedidosImportadosVO.getProperty("CODTIPVENDA");
		
		
		
		BigDecimal nuNotaModelo = null;

		if (codipoper.intValue() == 1111) {
			nuNotaModelo = new BigDecimal(129854618);
		} else if (codipoper.intValue() == 1015) {
			nuNotaModelo = new BigDecimal(129826009);
		} else if (codipoper.intValue() == 1011) {
			nuNotaModelo = new BigDecimal(129861670);
		} else {
			nuNotaModelo = new BigDecimal(218022);
		}

		System.out.println("NOTA MODELO ------------------>>>" + nuNotaModelo);

		codUsu = contexto.getUsuarioLogado();

		if (nunota == null) {
			
			try {
				
				nunota = criaCabecalho(nuNotaModelo, contexto);
				PersistentLocalEntity persistentLocalEntity = dwfFacade.findEntityByPrimaryKey("CabecalhoNota", nunota);
				EntityVO NVO = persistentLocalEntity.getValueObject();
				DynamicVO NotaGeradaVO = (DynamicVO) NVO;
				
				//pegando as info da top
				DynamicVO topRVO = ComercialUtils.getTipoOperacao(codipoper);
				Date dhtipoper = topRVO.asTimestamp("DHALTER");
				String tipoMovimento = topRVO.asString("TIPMOV");
				dataTop = dhtipoper;
				
				//pegando as info do tipo de neg
				DynamicVO tipNEG = ComercialUtils.getTipoNegociacao(codtipvenda);
				Timestamp dhtipvenda = tipNEG.asTimestamp("DHALTER");
				dataTipNeg = dhtipvenda;

				NotaGeradaVO.setProperty("CIF_FOB", "C");
				NotaGeradaVO.setProperty("CODUSU", codUsu);
				NotaGeradaVO.setProperty("CODCENCUS", pedidosImportadosVO.getProperty("CODCENCUS"));
				NotaGeradaVO.setProperty("CODEMP", pedidosImportadosVO.getProperty("CODEMP"));
				NotaGeradaVO.setProperty("CODNAT", pedidosImportadosVO.getProperty("CODNAT"));
				NotaGeradaVO.setProperty("CODPARC", pedidosImportadosVO.getProperty("CODPARC"));
				NotaGeradaVO.setProperty("CODPARCTRANSP", pedidosImportadosVO.getProperty("CODPARCTRANSP"));
				NotaGeradaVO.setProperty("CODTIPOPER", pedidosImportadosVO.getProperty("CODTIPOPER"));
				NotaGeradaVO.setProperty("CODTIPVENDA", pedidosImportadosVO.getProperty("CODTIPVENDA"));
				NotaGeradaVO.setProperty("NUMCONTRATO", pedidosImportadosVO.getProperty("NUMCONTRATO"));
				NotaGeradaVO.setProperty("OBSERVACAO", pedidosImportadosVO.getProperty("OBSERVACAO"));
				NotaGeradaVO.setProperty("DHTIPOPER", dhtipoper);
				NotaGeradaVO.setProperty("PENDENTE", new String("S"));
				NotaGeradaVO.setProperty("TIPMOV", tipoMovimento);
				NotaGeradaVO.setProperty("DHTIPVENDA", dhtipvenda);
				
				String prevEntrega = pedidosImportadosVO.asString("PREVENTREGA");
				if(prevEntrega!=null){
					NotaGeradaVO.setProperty("DTPREVENT",dataFormatada(pedidosImportadosVO.asString("PREVENTREGA")));	
				}

				persistentLocalEntity.setValueObject(NVO);
				
			} catch (Exception e) {
				contexto.mostraErro("Não foi possivel gerar o cabeçaho da nota!"+e);
			}
			

		}

		return nunota;

	}

	public BigDecimal criaCabecalho(BigDecimal nuModelo, ContextoAcao contexto) throws Exception {

		BigDecimal nunota = new java.math.BigDecimal(0);

		try {

			BigDecimal nuNotaModelo = (BigDecimal) nuModelo;

			dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO padraoNPVO = dwfFacade.getDefaultValueObjectInstance(DynamicEntityNames.CABECALHO_NOTA);
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("CabecalhoNota", nuNotaModelo);
			DynamicVO NotaProdVO = (DynamicVO) padraoNPVO;

			BigDecimal codTipOper = ModeloNPVO.asBigDecimal("CODTIPOPER");

			DynamicVO topRVO = ComercialUtils.getTipoOperacao(codTipOper);
			String tipoMovimento = topRVO.asString("TIPMOV");

			NotaProdVO.setProperty("CODTIPOPER", codTipOper);
			NotaProdVO.setProperty("TIPMOV", tipoMovimento);
			NotaProdVO.setProperty("SERIENOTA", ModeloNPVO.asString("SERIENOTA"));
			NotaProdVO.setProperty("CODPARC", ModeloNPVO.asBigDecimal("CODPARC"));
			NotaProdVO.setProperty("CODTIPVENDA", ModeloNPVO.asBigDecimal("CODTIPVENDA"));
			NotaProdVO.setProperty("CODEMP", ModeloNPVO.asBigDecimal("CODEMP"));
			NotaProdVO.setProperty("CODNAT", ModeloNPVO.asBigDecimal("CODNAT"));
			NotaProdVO.setProperty("CODCENCUS", ModeloNPVO.asBigDecimal("CODCENCUS"));
			NotaProdVO.setProperty("NUMNOTA", new java.math.BigDecimal(0));
			NotaProdVO.setProperty("APROVADO", ModeloNPVO.asString("APROVADO"));
			NotaProdVO.setProperty("PENDENTE", "N");
			NotaProdVO.setProperty("CIF_FOB", ModeloNPVO.asString("CIF_FOB"));
			NotaProdVO.setProperty("CODEMPNEGOC", ModeloNPVO.asBigDecimal("CODEMP"));
			NotaProdVO.setProperty("TIPFRETE", "N");

			dwfFacade.createEntity(DynamicEntityNames.CABECALHO_NOTA, (EntityVO) NotaProdVO);
			nunota = NotaProdVO.asBigDecimal("NUNOTA");

			return nunota;

		} catch (Exception e) {
			this.erro = e.getMessage();
			contexto.mostraErro("Problema ao criar cabecalho!!");
			e.printStackTrace();
		}

		return nunota;

	}

	public void inserirItem(BigDecimal nunota, ContextoAcao contexto, BigDecimal id) throws Exception {

		dwfFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO pedidosImportadosVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("AD_PEDIDOSIMPORTADOS",
				new Object[] { id });

		// NUNOTA,CODPROD,CODEMP,CODLOCALORIG,QTDNEG,VLRUNIT,CODVOL,SEQUENCIA

		BigDecimal codlocalorig = BigDecimal.ZERO;

		BigDecimal codemp = (BigDecimal) pedidosImportadosVO.getProperty("CODEMP");
		codlocalorig = (BigDecimal) pedidosImportadosVO.getProperty("CODLOCALORIG");
		BigDecimal codprod = (BigDecimal) pedidosImportadosVO.getProperty("CODPROD");
		BigDecimal vlrunit = (BigDecimal) pedidosImportadosVO.getProperty("VLRUNIT");
		BigDecimal qtdneg = (BigDecimal) pedidosImportadosVO.getProperty("QTDNEG");
		BigDecimal codtipoper = (BigDecimal) pedidosImportadosVO.getProperty("CODTIPOPER");
		BigDecimal idtabela = (BigDecimal) pedidosImportadosVO.getProperty("IDPEDIDO");
		BigDecimal pedido2 = (BigDecimal) pedidosImportadosVO.getProperty("NUMPEDIDO2");

		dwfFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO cadProdutosVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("Produto", new Object[] { codprod });
		String codvol = cadProdutosVO.asString("CODVOL");
		String usoProd = (String) cadProdutosVO.asString("USOPROD");

		BigDecimal codlocalpadrao = cadProdutosVO.asBigDecimal("CODLOCALPADRAO");

		if (codlocalorig == null || codlocalorig.intValue() == 0) {
			codlocalorig = codlocalpadrao;

			if (codlocalorig == null || codlocalorig.intValue() == 0) {
				codlocalorig = new BigDecimal(1110);
			}

		}

		// verifica se precisa validar estoque
		DynamicVO topRVO = ComercialUtils.getTipoOperacao(codtipoper);
		String topAtualEst = topRVO.asString("ATUALEST");

		if (topAtualEst.equals("R") || topAtualEst.equals("B")) {
			validaEstoque(contexto, codemp, codlocalorig, codprod, "", qtdneg, idtabela);
		}

		if (nunota != null) {
			DynamicVO itemVO = (DynamicVO) dwfFacade.getDefaultValueObjectInstance(DynamicEntityNames.ITEM_NOTA);

			itemVO.setProperty("CODEMP", codemp);
			itemVO.setProperty("CODPROD", codprod);
			itemVO.setProperty("NUNOTA", nunota);
			itemVO.setProperty("VLRUNIT", vlrunit);
			itemVO.setProperty("QTDNEG", qtdneg);
			itemVO.setProperty("CODVOL", codvol);
			itemVO.setProperty("CODLOCALORIG", codlocalorig);
			itemVO.setProperty("VLRTOT", vlrunit.multiply(qtdneg).setScale(3, BigDecimal.ROUND_HALF_UP));
			itemVO.setProperty("ATUALESTOQUE", atualEst(codtipoper));
			itemVO.setProperty("USOPROD", usoProd);
			
			if(pedido2!=null){
				itemVO.setProperty("NUMPEDIDO2", pedido2.toString());
			}
			
			dwfFacade.createEntity(DynamicEntityNames.ITEM_NOTA, (EntityVO) itemVO);
		}

	}

	public void salvarRetorno(BigDecimal id, BigDecimal nunota) throws Exception {

		Timestamp dthoje = new Timestamp(new Date().getTime());

		PersistentLocalEntity persistentLocalEntity = dwfFacade.findEntityByPrimaryKey("AD_PEDIDOSIMPORTADOS",
				new Object[] { id });
		EntityVO RVO = persistentLocalEntity.getValueObject();
		DynamicVO retornoVO = (DynamicVO) RVO;

		retornoVO.setProperty("NUNOTA", nunota);
		retornoVO.setProperty("PEDIDOGERADO", new String("S"));
		retornoVO.setProperty("DTALTER", dthoje);
		retornoVO.setProperty("DTNEG", dthoje);
		retornoVO.setProperty("CODUSU", codUsu);
		retornoVO.setProperty("DHTOP", dataTop);
		retornoVO.setProperty("DHTIPVENDA", dataTipNeg);

		persistentLocalEntity.setValueObject(RVO);
		
		dataTop = null;
		dataTipNeg = null;

	}

	public BigDecimal atualEst(BigDecimal codtipoper) throws Exception {

		DynamicVO topRVO = ComercialUtils.getTipoOperacao(codtipoper);

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

		// System.out.println("Metodo 1:"+atualEst);

		return atualEst;

	}

	private void validaEstoque(ContextoAcao contexto, BigDecimal codemp, BigDecimal codlocal, BigDecimal codprod,
			String controle, BigDecimal qtdneg, BigDecimal idtabela) throws Exception {

		ComercialUtils.ResultadoValidaEstoque ValidaEstoque = ComercialUtils.validaEstoque(codemp, codlocal, codprod,
				controle, "N");
		BigDecimal Estoque = ValidaEstoque.getQtdEst();
		if (Estoque.compareTo(qtdneg) == -1) {
			contexto.mostraErro("ATENÇÃO: Estoque Insuficiente! \r\n Produto:" + codprod.toString() + "\r\nLocal:"
					+ codlocal.toString() + "\r\nEstoque:" + Estoque + "\r\nId Pedido: " + idtabela);
		}
	}
	
	private Timestamp dataFormatada(String data) throws ParseException{

			SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy");
			Date dataFormatada = formato.parse(data);
			
			Timestamp tmFormatada = new Timestamp(dataFormatada.getTime());
			
			return tmFormatada;		
		
	}

}

