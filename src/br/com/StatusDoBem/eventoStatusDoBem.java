package br.com.StatusDoBem;

import java.math.BigDecimal;
import java.sql.ResultSet;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class eventoStatusDoBem implements EventoProgramavelJava{
	
	/**
	 * @author gabriel.nascimento
	 * 
	 * Objeto responsavel por alterar o status do bem na tabela TGFLOT.
	 * 
	 * Existe um campo na tabela TCIBEM (AD_STATUS) que uma vez que for alterado deve referir no campo (TEXTO) da tabela TGFLOT.
	 * Existe a validação prévia para verificar se o bem existe em estoque, do contrário deve ser inserido por uma nota de entrada ou de acerto top sugerida (1531)
	 */
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		start(arg0);
	}
	
	public void start(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal codProduto = VO.asBigDecimal("CODPROD");
		String status = VO.asString("AD_STATUS");
		String patrimonio = VO.asString("CODBEM");
		
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		String oldStatus = oldVO.asString("AD_STATUS");
		
		if(status!=oldStatus && status!=null) {
			
			if(validaSeExisteNaTGFEST(codProduto,patrimonio)) {
				
				if(validaSeExisteNaTCIEST(codProduto)) {
					if(validaSeExisteNaTGFLOT(codProduto,patrimonio)) {
						deletarTGFLOT(patrimonio);
						cadastrarStatus(codProduto,patrimonio,status);
					}else {
						cadastrarStatus(codProduto,patrimonio,status);
					}
				}else {
					throw new PersistenceException(
							"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
							"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>O Produto "+codProduto+" não possui configuração para a Estrutura do controle adicional!!</b></font>\n\n <b>(Aba Medidas e estoque / Controle Adicional, botão Estrutura).</b>\n\n\n");
				}	
				
			}else {				
				throw new PersistenceException(
						"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
						"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>O bem "+patrimonio+" com o produto "+codProduto+" não foi localizado no estoque, será necessário inseri-lo!!</b></font>\n\n <b>A inserção pode ser feita no portal de compras, dando entrada em sua nota de compra ou por uma TOP que realize o ajuste de estoque</b>\n\n\n");
			}
		}
	}
	
	public boolean validaSeExisteNaTGFEST(BigDecimal produto,String patrimonio) throws Exception {
		boolean valida=false;
		
		JapeWrapper DAO = JapeFactory.dao("Estoque");
		DynamicVO VO = DAO.findOne("this.CODPROD=? AND this.CONTROLE=?",new Object[] { produto,patrimonio });
		
		if(VO!=null) {
			valida=true;
		}
		return valida;
	}
	
	public boolean validaSeExisteNaTCIEST(BigDecimal produto) throws Exception {
		boolean valida=false;
		
		JapeWrapper DAO = JapeFactory.dao("EstruturaLote");
		DynamicVO VO = DAO.findOne("this.CODPROD=?",new Object[] { produto});
		
		if(VO!=null) {
			valida=true;
		}
		return valida;
	}
	
	
	public boolean validaSeExisteNaTGFLOT(BigDecimal produto,String patrimonio) throws Exception {
		boolean valida=false;
		
		JapeWrapper DAO = JapeFactory.dao("Lote");
		DynamicVO VO = DAO.findOne("this.CODPROD=? AND CONTROLE=?",new Object[] { produto,patrimonio });
		
		if(VO!=null) {
			valida=true;
		}
		return valida;
	}
	
	public void deletarTGFLOT(String patrimonio) throws Exception {
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("Lote", "this.CONTROLE=?",new Object[] {patrimonio}));
		} catch (Exception e) {
			System.out.println("** NAO FOI POSSIVEL EXCLUIR O LOTE! **"+e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	public void cadastrarStatus(BigDecimal produto, String patrimonio, String status) throws Exception {
		
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("Lote");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODPROD", produto);
			VO.setProperty("CODEMP", verificaEmpresaDoContrato(patrimonio));
			VO.setProperty("CODLOCAL", new BigDecimal(1500));
			VO.setProperty("CONTROLE", patrimonio);
			VO.setProperty("TITULO", "Status");
			VO.setProperty("LOGICO", "S");
			VO.setProperty("TEXTO", status);
			
			dwfFacade.createEntity("Lote", (EntityVO) VO);
			
		} catch (Exception e) {
			System.out.println("** NAO FOI POSSIVEL CADASTRAR O STATUS DO BEM! **"+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public BigDecimal verificaEmpresaDoContrato(String patrimonio) throws Exception {
		BigDecimal empresa = null;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT CODEMP FROM TCSCON WHERE NUMCONTRATO IN (SELECT NUMCONTRATO FROM TCIBEM WHERE CODBEM='"+patrimonio+"')");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			empresa = contagem.getBigDecimal("CODEMP");
		}
		
		if(empresa==null) {
			empresa = new BigDecimal(2);
		}
		
		return empresa;

	}

}
