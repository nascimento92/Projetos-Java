package br.com.teclas;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
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
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class eventoTeclas implements EventoProgramavelJava {

	/**
	 * Objeto tem o objeto de facilitar o cadastro de teclas/produtos
	 * ele verifica se o produto da tecla já existe na produtos/serviços se existe ele atualiza o preço se n ele cadastra
	 * 02/09/2019 - teste da função com as áreas interessadas
	 */
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		
		
	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		alteraProdutoQueJaExiste(arg0);
		
	}

	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		start(arg0);
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		
		
	}
	
	public void start(PersistenceEvent arg0) throws Exception{
		
		DynamicVO teclasVo = (DynamicVO) arg0.getVo();
		
		BigDecimal produto = teclasVo.asBigDecimal("CODPROD");
		String codbem = teclasVo.asString("CODBEM");
		BigDecimal tecla = teclasVo.asBigDecimal("TECLA");
		BigDecimal contrato = teclasVo.asBigDecimal("NUMCONTRATO");
		BigDecimal vlrpar = teclasVo.asBigDecimal("VLRPAR");
		BigDecimal vlrfuncionario = teclasVo.asBigDecimal("VLRFUN");
		BigDecimal contratoPatrimonio = null;
		
		if(codbem!=null) {
			contratoPatrimonio = getNumContrato(codbem);
		}
		
		try {
			
			if(contrato!=null && contratoPatrimonio!=null) {
				if(contrato.equals(contratoPatrimonio)) {
					if(codbem!=null && tecla!=null && produto!=null && vlrfuncionario!=null && vlrpar!=null) {
						//valida se já existe esta tecla para este patrimonio
						validaSeTeclaJaExiste(contrato,codbem,tecla);
						
						if(validaSeExisteNaTCSPSC(produto,contrato)){
							alteraProdutoQueJaExiste(arg0);
						}else{	
							salvaProduto(contrato,produto,vlrfuncionario,vlrpar); //tem q salvar na tcspre
							alteraProdutoQueJaExiste(arg0);
						}
					}
				}
			}
			
			//valida se o patrimonio está no contrato
			/*if(contratoPatrimonio.intValue()!=contrato.intValue()){
				String erro = "O Patrimônio: "+codbem+"\n\nNão se encontra no contrato: "+contrato;
				myException(erro);
			}
			*/

		} catch (Exception e) {
			salvarException("[start] Nao foi possivel realizar as alterações! patrimonio: "+codbem+" tecla: "+tecla+"\n"+e.getMessage()+"\n"+e.getCause());
		}
		
	}
	
	public BigDecimal getNumContrato(String codbem) throws Exception{
		
		BigDecimal retorno = new BigDecimal(0);
		
		JapeWrapper tgfcabDAO = JapeFactory.dao("PATRIMONIO");
		DynamicVO tcibemVO = tgfcabDAO.findOne("CODBEM=?",new Object[] { codbem });
		
		BigDecimal numcontrato = tcibemVO.asBigDecimal("NUMCONTRATO");
		
		if(numcontrato!=null && numcontrato.intValue()>0){
			return numcontrato;
		}else{
			return retorno;
		}	
	}
	
	public boolean validaSeExisteNaTCSPSC(BigDecimal codprod, BigDecimal numcontrato) throws Exception{
		
		boolean valida = false;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT COUNT(*) FROM TCSPSC WHERE CODPROD="+codprod+" AND NUMCONTRATO="+numcontrato);
		contagem = nativeSql.executeQuery();
		

		while (contagem.next()) {
			int count = contagem.getInt("COUNT(*)");

			if (count >= 1) {
				valida = true;
			}
		}
		
		return valida;
	}
	
	public void salvaProduto(BigDecimal numcontrato, BigDecimal codprod, BigDecimal vlrfuncionario, BigDecimal vlrparceiro) throws Exception{
		
		try {
			
			//TCSPSC
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO padraoNPVO = dwfFacade.getDefaultValueObjectInstance(DynamicEntityNames.PRODUTO_SERVICO_CONTRATO);
			DynamicVO prodservicoVO = (DynamicVO) padraoNPVO;
			
			prodservicoVO.setProperty("NUMCONTRATO", numcontrato);
			prodservicoVO.setProperty("CODPROD", codprod);
			prodservicoVO.setProperty("AD_FRANQUIA", "S");
			
			dwfFacade.createEntity(DynamicEntityNames.PRODUTO_SERVICO_CONTRATO, (EntityVO) prodservicoVO);
			
			//TCSPRE
			BigDecimal tot = vlrfuncionario.add(vlrparceiro);
			Timestamp dataAtual = new Timestamp(System.currentTimeMillis());
			
			EntityVO padrao2NPVO = dwfFacade.getDefaultValueObjectInstance(DynamicEntityNames.PRECO_CONTRATO);
			DynamicVO precoVO = (DynamicVO) padrao2NPVO;
			
			precoVO.setProperty("NUMCONTRATO", numcontrato);
			precoVO.setProperty("CODPROD", codprod);
			precoVO.setProperty("VALOR", tot);
			precoVO.setProperty("REFERENCIA", dataAtual);
			
			dwfFacade.createEntity(DynamicEntityNames.PRECO_CONTRATO, (EntityVO) precoVO);
			
		} catch (Exception e) {
			salvarException("[salvaProduto] Nao foi possivel salvar o produto! contrato: "+numcontrato+" produto: "+codprod+"\n"+e.getMessage()+"\n"+e.getCause());
		}	
		
	}
	
	public void validaSeTeclaJaExiste(BigDecimal numcontrato, String codbem, BigDecimal tecla) throws Exception{
	
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT COUNT(*) FROM AD_TECLAS WHERE NUMCONTRATO="
				+ numcontrato + " AND CODBEM='" + codbem + "' AND TECLA="
				+ tecla);
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			int count = contagem.getInt("COUNT(*)");

			if (count >= 1) {
				
				String erro = "A tecla "+tecla+" já foi cadastrada para o Patrimônio: "+codbem;
				myException(erro);
			}
		}
	}
	
	public void myException(String erro) throws PersistenceException{
		throw new PersistenceException(
		"<p align=\"center\">"
		+ "<img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"
		+ "\n\n\n\n<font size=\"15\" color=\"#008B45\">"
		+ "<b>"+erro+"</b></font>\n\n\n");
	}
	
	public void alteraProdutoQueJaExiste(PersistenceEvent arg0)	throws Exception {
		
		DynamicVO precoVO = (DynamicVO) arg0.getVo();
		
		BigDecimal produto = precoVO.asBigDecimal("CODPROD");
		BigDecimal contrato = precoVO.asBigDecimal("NUMCONTRATO");
		BigDecimal vlrpar = precoVO.asBigDecimal("VLRPAR");
		BigDecimal vlrfuncionario = precoVO.asBigDecimal("VLRFUN");
		
		BigDecimal total = vlrpar.add(vlrfuncionario);
		
		Timestamp dataAtual = new Timestamp(System.currentTimeMillis());
		
		Date dtAtual = new Date(System.currentTimeMillis());
		SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy");
		String dataFormatada = formato.format(dtAtual);
		
		//select para ver se na existe na TCSPRE
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT COUNT(*) FROM TCSPRE WHERE CODPROD="+produto+" AND NUMCONTRATO="+contrato+" AND REFERENCIA = TO_DATE('"+dataFormatada+"')");
		contagem = nativeSql.executeQuery();
		
		while (contagem.next()) {
			int count = contagem.getInt("COUNT(*)");

			if (count >= 1) {
				atualizaReferencia(contrato,produto,total,dataFormatada);
			}else{
				novaReferencia(contrato,produto,total,dataAtual);
			}
		}
		
		//altera as outras teclas onde existe este produto
		Collection<?> teclas = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("teclas","this.NUMCONTRATO=? AND this.CODPROD=? ", new Object[] { contrato,produto }));

		for (Iterator<?> Iterator = teclas.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO VO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			
			VO.setProperty("VLRFUN", vlrfuncionario);
			VO.setProperty("VLRPAR", vlrpar);
			
			itemEntity.setValueObject((EntityVO) VO);
		}

		}
	
	private void novaReferencia(BigDecimal contrato, BigDecimal produto, BigDecimal total, Timestamp dataAtual) throws Exception{
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO padrao2NPVO = dwfEntityFacade.getDefaultValueObjectInstance("PrecoContrato");
			DynamicVO tcspreVO = (DynamicVO) padrao2NPVO;

			tcspreVO.setProperty("NUMCONTRATO", contrato);
			tcspreVO.setProperty("CODPROD", produto);
			tcspreVO.setProperty("VALOR", total);
			tcspreVO.setProperty("REFERENCIA", dataAtual);

			dwfEntityFacade.createEntity("PrecoContrato",(EntityVO) tcspreVO);
			
		} catch (Exception e) {
			salvarException("[novaReferencia] Nao foi possivel salvar a referencia! contrato: "+contrato+" produto: "+produto+"\n"+e.getMessage()+"\n"+e.getCause());
		}	
		
	}
	
	private void atualizaReferencia(BigDecimal contrato, BigDecimal produto, BigDecimal total, String data) throws Exception{
		
		try {
			
			//altera a tcspre
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> preco = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("PrecoContrato","this.NUMCONTRATO=? AND this.CODPROD=? AND this.REFERENCIA=? ", new Object[] { contrato,produto,data }));

			for (Iterator<?> Iterator = preco.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO VO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
							
			VO.setProperty("VALOR", total);
			
			itemEntity.setValueObject((EntityVO) VO);
			
			}
			
		} catch (Exception e) {
			salvarException("[atualizaReferencia] Nao foi possivel atualizar a referencia! contrato: "+contrato+" produto: "+produto+"\n"+e.getMessage()+"\n"+e.getCause());
		}
		
	}
	
	private void salvarException(String mensagem) {
		try {
			
			BigDecimal usuario = ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID();
			if(usuario==null) {
				usuario = new BigDecimal(0);
			}

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "eventoTeclas");
			VO.setProperty("PACOTE", "br.com.teclas");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", usuario);
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}
}

