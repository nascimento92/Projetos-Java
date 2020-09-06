package br.com.BEC;

import java.math.BigDecimal;
import java.sql.ResultSet;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
//import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btnIncluirParceiroBEC implements AcaoRotinaJava{

	public void doAction(ContextoAcao arg0) throws Exception {
		
		/*
		 * Registro[] linhas = arg0.getLinhas();
		 * 
		 * if(linhas.length>1){ arg0.
		 * mostraErro("<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"
		 * +
		 * "<font size=\"15\">\n<b>Selecione apenas <u>um</u> Parceiro para ser incluido</b></font>"
		 * ); }else{ cadastrarParceiroNaRegra(linhas,arg0); }
		 */
		start(arg0);
	}
	
	private void start(ContextoAcao arg0) throws Exception {
		
		Integer p = (Integer) arg0.getParam("CODPARC");
		
		BigDecimal parceiro = new BigDecimal(p);
		
		if(parceiro!=null) {
			cadastrarParceiroNaRegra(parceiro,arg0);
		}
		
	}
	
	private void cadastrarParceiroNaRegra(BigDecimal codparc, ContextoAcao arg0) throws Exception{
		//BigDecimal codparc = (BigDecimal) linhas[0].getCampo("CODPARC");
		BigDecimal regra = new BigDecimal(61);
		
		if(!verificaSeOhParceiroJaEstaCadastrado(codparc,regra,arg0)){
			System.out.println("cadastrar o parceiro");
			cadastrarParceiro(codparc,regra, arg0);
		}else{
			arg0.setMensagemRetorno("Parceiro: <b>"+codparc+"</b> já está cadastrado na regra: <b>"+regra+"</b>");
		}
	}
	
	private boolean verificaSeOhParceiroJaEstaCadastrado(BigDecimal codparc, BigDecimal regra, ContextoAcao arg0) throws Exception{
		boolean valida = false;
		
		JapeWrapper DAO = JapeFactory.dao("ItemRegras");
		DynamicVO VO = DAO.findOne("CODINSTPRINC=? AND CODINSTPRINCFIN=? AND CODREGRA=? ",new Object[] { codparc, codparc, regra });

		if(VO!=null){
			return valida = true;
		}

		return valida;
	}
	
	private void cadastrarParceiro(BigDecimal codparc, BigDecimal regra, ContextoAcao arg0) throws Exception{
		
		BigDecimal sequencia = pegaUltimaSequencia(regra);
		BigDecimal adicao = new BigDecimal(1);
		
		adicao = sequencia.add(adicao);
		
		//insert
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("ItemRegras");
			DynamicVO VO2 = (DynamicVO) NPVO;
			
			VO2.setProperty("CODREGRA", regra);
			VO2.setProperty("SEQUENCIA", adicao);
			VO2.setProperty("CODINSTPRINC", codparc);
			VO2.setProperty("CODINSTSECINI", new BigDecimal(0));
			VO2.setProperty("CODINSTSECFIN", new BigDecimal(0));
			VO2.setProperty("ATIVO", "S");
			VO2.setProperty("CODINSTPRINCFIN", codparc);
			
			dwfFacade.createEntity("ItemRegras", (EntityVO) VO2);
			
			arg0.setMensagemRetorno("Parceiro: "+codparc+" incluido na Regra: "+regra);
			
		} catch (Exception e) {
			System.out.println("ERRO AO INSERIR O ARQUIVO! "+e.getMessage());
		}
		

	}
	
	private BigDecimal pegaUltimaSequencia(BigDecimal regra) throws Exception{
		
		BigDecimal sequencia = new BigDecimal(0);
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT NVL(MAX(SEQUENCIA),0) AS SEQUENCIA FROM TGFITR WHERE CODREGRA="+regra);
		contagem = nativeSql.executeQuery();
		

		while (contagem.next()) {
			sequencia = contagem.getBigDecimal("SEQUENCIA");

		}
		
		return sequencia;
	}

}
