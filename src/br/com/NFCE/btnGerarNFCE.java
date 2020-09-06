package br.com.NFCE;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.BigDecimalUtil;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.BarramentoRegra;
import br.com.sankhya.modelcore.comercial.ConfirmacaoNotaHelper;
import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
import br.com.sankhya.modelcore.dwfdata.vo.CabecalhoNotaVO;
import br.com.sankhya.modelcore.dwfdata.vo.tgf.TipoOperacaoVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btnGerarNFCE implements AcaoRotinaJava {
	/**
	 * Botão não esta sendo utilizado
	 */
	private EntityFacade dwfEntityFacade = null;
	private JdbcWrapper jdbcWrapper = null;
	private int contador = 0;
	
	public void doAction(ContextoAcao contexto) throws Exception {
		
		localizaAsNFCE(contexto);
		contexto.setMensagemRetorno(contador+" Notas Atualizadas!");
		
	}
	
	//Atualiza os produtos da nota e salva na tabela de preço.
	
	public void localizaAsNFCE(ContextoAcao contexto) throws Exception{
			
		dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("CabecalhoNota",
				"this.CODTIPOPER=? AND this.PENDENTE=? AND this.STATUSNOTA=? AND this.CODVEND=?", new Object[] { new BigDecimal(1108), "S", "A",new BigDecimal(280)}));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

		PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
		DynamicVO NVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

		BigDecimal nunota = NVO.asBigDecimal("NUNOTA");
		alteraProdutosDaNota(nunota, NVO);
		
		totalizaImpostos(nunota);
		confirmarNota(nunota,contexto);
		contador++;
		}
		
	}
	
	public void alteraProdutosDaNota(BigDecimal nunota, DynamicVO tgfcabVO) throws Exception{
		
		dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("ItemNota","this.NUNOTA=?", new Object[] { nunota }));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

		PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
		EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
		DynamicVO VO = (DynamicVO) NVO;
		
		BigDecimal codprod = VO.asBigDecimal("CODPROD");
		BigDecimal valor = VO.asBigDecimal("AD_VLRUN");
		
		if(valor.equals(null)){
			valor = new BigDecimal(1);
		}
		atualizaTabelaDePrecos(codprod,valor);
		
		VO.setProperty("VLRUNIT", valor);
		
		itemEntity.setValueObject(NVO);
		
		CabecalhoNotaVO notaVO = loadNotaVO(nunota);
		BigDecimal vlrnota = notaVO.asBigDecimal("VLRNOTA");
		TipoOperacaoVO topVO = (TipoOperacaoVO)((DynamicVO)notaVO.getTipoOperacao()).wrapInterface(TipoOperacaoVO.class);
		
		//ItemNotaHelpper.calcularTotalItens(notaVO, "N", "S");
		//ItemNotaHelpper.calcularTotalNota(notaVO, topVO, "N", vlrnota, 0);

		}
	}
	
	public void atualizaTabelaDePrecos(BigDecimal codprod, BigDecimal valor) throws Exception{
		
		if(descobreSeOhProdutoJaFoiCadastradoNaTabelaDePreco(codprod)){
			System.out.println("atualiza o preço");
			atualizaPreco(codprod,valor);
		}else{
			System.out.println("cadastra na tabela");
			cadastraProduto(codprod,valor);
		}
	}
	
	public boolean descobreSeOhProdutoJaFoiCadastradoNaTabelaDePreco(BigDecimal codprod) throws Exception{
		
		boolean valida = false;
			
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT COUNT(*) FROM TGFEXC WHERE CODPROD="+codprod+" AND NUTAB=1704");
		contagem = nativeSql.executeQuery();
		

		while (contagem.next()) {
			int count = contagem.getInt("COUNT(*)");

			if (count >= 1) {
				valida = true;
			}
		}
		
		return valida;
	}
	
	public void atualizaPreco(BigDecimal codprod, BigDecimal valor) throws Exception{
		
		dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("Excecao","this.CODPROD=? AND this.NUTAB=? ", new Object[] {codprod,"1704"}));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

		PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
		EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
		DynamicVO VO = (DynamicVO) NVO;
		
		VO.setProperty("VLRVENDA", valor);
		
		itemEntity.setValueObject(NVO);

			}
		}
	
	public void cadastraProduto(BigDecimal codprod, BigDecimal valor) throws Exception{
		
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO EVO = dwfFacade.getDefaultValueObjectInstance("Excecao");
		DynamicVO VO = (DynamicVO) EVO;
		
		VO.setProperty("CODLOCAL", new BigDecimal(0));
		VO.setProperty("CODPROD", codprod);
		VO.setProperty("NUTAB", new BigDecimal(1704));
		VO.setProperty("TIPO", "V");
		VO.setProperty("VLRVENDA", valor);
		
		dwfFacade.createEntity("Excecao", (EntityVO) EVO);
	}
	
	
	//Altera a nota confirmando e recalculando os impostos.
	
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
	
	public void confirmarNota(BigDecimal nunota,ContextoAcao contexto)throws Exception {
		
		BigDecimal nuNota = BigDecimalUtil.getValueOrZero(nunota);
		
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
			contexto.mostraErro("Não foi possivel confirmar a nota: "+e);
		} 
	}
	
	private static CabecalhoNotaVO loadNotaVO(BigDecimal nuNota) throws Exception {
	    PersistentLocalEntity entity = null;
	    EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
	    entity = dwfEntityFacade.findEntityByPrimaryKey("CabecalhoNota", nuNota);
	    return (CabecalhoNotaVO)((DynamicVO)entity.getValueObject()).wrapInterface(CabecalhoNotaVO.class);
	  }
}

