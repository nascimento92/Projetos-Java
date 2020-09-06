package Travas;

import java.math.BigDecimal;
import java.sql.ResultSet;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class validaFornecedoresDuplicados implements EventoProgramavelJava {

	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {

	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		if (ehFornecedor(arg0)) {
			
			if(ehEstrangeiro(arg0)==false){
				
				if(validaSeCnpjIgualZero(arg0)==false){
					validaSeExisteDuplicado(arg0);
				}
				
			}	
			
		}

	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		
		if(ehEstrangeiro(arg0)==false){
			
			if(validaSeCnpjIgualZero(arg0)==false){
				System.out.println("***** GC_VALIDA FORNECEDOR DUPLICADO *****");
				validaSeEstaAlterandoFornecedor(arg0);
			}		
		}
		
		

	}

	public boolean ehFornecedor(PersistenceEvent arg0) {
		boolean valida = false;

		DynamicVO tgfparVO = (DynamicVO) arg0.getVo();

		String fornecedor = tgfparVO.asString("FORNECEDOR");

		if ("S".equals(fornecedor)) {
			valida = true;
		} else {
			valida = false;
		}

		return valida;
	}
	
	public boolean ehEstrangeiro(PersistenceEvent arg0) throws Exception{
		boolean valida = false;

		DynamicVO tgfparVO = (DynamicVO) arg0.getVo();

		BigDecimal codcid = tgfparVO.asBigDecimal("CODCID");
		
		JapeWrapper RecorrenciaDAO = JapeFactory.dao("Cidade");
		DynamicVO cidade = RecorrenciaDAO.findOne("CODCID=?",new Object[] { codcid });
		
		BigDecimal codmunfis = cidade.asBigDecimal("CODMUNFIS");
		BigDecimal coduf = cidade.asBigDecimal("UF");
		
		if(codmunfis!=null){	
			if(coduf!=null){
				if (codmunfis.equals(new BigDecimal(9999999)) && coduf.equals(new BigDecimal(32))){
					return valida = true;
				}
			}	
		}

		return valida;
	}
	
	public void validaSeEstaAlterandoFornecedor(PersistenceEvent arg0) throws Exception{
		
		DynamicVO tgfparOldVO = (DynamicVO) arg0.getOldVO();
		DynamicVO tgfparVO = (DynamicVO) arg0.getVo();
		
		String fornecedorOLD = tgfparOldVO.asString("FORNECEDOR");
		String fornecedorNEW = tgfparVO.asString("FORNECEDOR");
		String ativoOLD = tgfparOldVO.asString("ATIVO");
		String ativoNEW = tgfparVO.asString("ATIVO");
		
			
		if("N".equals(fornecedorOLD) && "S".equals(fornecedorNEW) && "S".equals(ativoNEW)){
			validaSeExisteDuplicado(arg0); 
		}
		
		if("N".equals(ativoOLD) && "S".equals(ativoNEW) && "S".equals(fornecedorNEW)){
			validaSeExisteDuplicado(arg0);	
		}
		
		if("N".equals(ativoNEW) && "N".equals(fornecedorOLD) && "S".equals(fornecedorNEW)){ 
			throw new PersistenceException("<b>\n\nNão é possivel marcar parceiros inativos como fornecedores!\n\n</b>");
		}
		
		/*if("S".equals(fornecedorNEW) && "S".equals(ativoNEW)){
			validaSeExisteDuplicado(arg0);	
		}*/
	
	}
	
	public void validaSeExisteDuplicado(PersistenceEvent arg0) throws Exception{
		
		DynamicVO contextoVO = (DynamicVO) arg0.getVo();

		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT COUNT(*) FROM TGFPAR WHERE CGC_CPF='"
				+ contextoVO.asString("CGC_CPF") + "' AND ATIVO='S' AND FORNECEDOR='S'");
		contagem = nativeSql.executeQuery();
		

		while (contagem.next()) {
			int count = contagem.getInt("COUNT(*)");

			if (count >= 1) {
				throw new PersistenceException(
						"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
						"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>Já existem "+count+" fornecedor(es) com este CNPJ: <u>"+contextoVO.asString("CGC_CPF")+
						"</u>\n\nNão é possivel cadastrar fornecedores duplicados!!</b></font>\n\n\n");
			}

		}
		
	}
	
	public boolean validaSeCnpjIgualZero(PersistenceEvent arg0){
		boolean valida = false;

		DynamicVO tgfparVO = (DynamicVO) arg0.getVo();

		String cnpj = tgfparVO.asString("CGC_CPF");
		BigDecimal cnpjInteiro = new BigDecimal(cnpj);

		if (cnpjInteiro.equals(new Integer(0))) {
			valida = true;
		} else if(cnpj.equals(new String("00000000000000"))){
			valida = true;
		} else {
			valida = false;
		}

		return valida;
	}


}

