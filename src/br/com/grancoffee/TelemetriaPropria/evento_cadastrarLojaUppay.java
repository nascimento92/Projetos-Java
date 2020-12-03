package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;

import com.sankhya.util.TimeUtils;

import Helpers.WSPentaho;
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class evento_cadastrarLojaUppay implements EventoProgramavelJava {

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
	
	private void insert(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal idTel = VO.asBigDecimal("IDTEL");
		String principal = VO.asString("PRINCIPAL");
		String patrimonio = VO.asString("CODBEM");
		DynamicVO gcInstalacao = getGcInstalacao(patrimonio);
		String micromarketing = gcInstalacao.asString("TOTEM");
		
		if(validacoes(patrimonio) && "S".equals(principal)) {
			throw new Error("<br/><br/><b>Não é possível cadastrar uma nova telemetria como principal com um pedido de abastecimento pendente!</b><br/><br/>");
		}
		
		if("S".equals(micromarketing) && idTel.intValue()==2 && "S".equals(principal)) {
			chamaPentahoCadastro();
			marcarEnviarPlanogramaNaTelaInstalacao(patrimonio,"S");
			chamaPentahoPlanograma();
			marcarEnviarPlanogramaNaTelaInstalacao(patrimonio,"N");
		}
	}
	
	private void update(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		
		BigDecimal idTel = VO.asBigDecimal("IDTEL");
		String principal = VO.asString("PRINCIPAL");
		String oldPrincipal = oldVO.asString("PRINCIPAL");
		String patrimonio = VO.asString("CODBEM");
		DynamicVO gcInstalacao = getGcInstalacao(patrimonio);
		String micromarketing = gcInstalacao.asString("TOTEM");
		String pinpad = VO.asString("AD_PINPADDIG");
		
		/*
		if(validacoes(patrimonio)==true && ("N".equals(oldPrincipal) || oldPrincipal==null) && "S".equals(principal)) {
			throw new Error("<br/><br/><b>Não é possível cadastrar uma nova telemetria como principal com um pedido de abastecimento pendente!</b><br/><br/>");
		}
		*/
		
		if("S".equals(micromarketing) && idTel.intValue()==2 && "S".equals(principal) && pinpad!=null) {
			chamaPentahoCadastro();
			//marcarEnviarPlanogramaNaTelaInstalacao(patrimonio,"S");
			//chamaPentahoPlanograma();
			//marcarEnviarPlanogramaNaTelaInstalacao(patrimonio,"N");
		}
		
	}
	
	//n esta funcionando
	//precisa validar se o pinpad digital e a box não estão instalados em outra máquina!
	//precisa descomentar a validação do método update
	
	private boolean validacoes(String patrimonio) {
		boolean valida = false;

		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT COUNT(*) FROM GC_SOLICITABAST WHERE STATUS='1' AND CODBEM='"+patrimonio+"'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				if (count >= 1) {
					valida = true;
				}
			}

		} catch (Exception e) {
			salvarException("[validacoes] Não foi possível validar!" + e.getMessage()+"\n"+e.getCause());
		}

		return valida;
	}
	
	private DynamicVO getGcInstalacao(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("GCInstalacao");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		return VO;
	}
	
	private void chamaPentahoCadastro() {
		
		try {
			
			String site = "http://pentaho.grancoffee.com.br:8080/pentaho/kettle/";
		    String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
		    WSPentaho si = new WSPentaho(site, Key);
		    		    
		    String path = "home/GC/Projetos/GCW/Transformations/";
		    String objName = "TF - GSN005 - Cadastra patrimonios no MID";
		    String objName2 = "TF - GSN009 - Criar Loja uppay";
		    
		    si.runTrans(path, objName);
		    si.runTrans(path, objName2);
		    		
		} catch (Exception e) {
			salvarException("[chamaPentahoCadastro] Não foi possível chamar a Rotina Pentaho de Cadastro!" + e.getMessage()+"\n"+e.getCause());
		}		
	}
	
	private void marcarEnviarPlanogramaNaTelaInstalacao(String patrimonio, String tipo) {
		try {
			JapeWrapper parceiroDAO = JapeFactory.dao("GCInstalacao");
			parceiroDAO.prepareToUpdateByPK(patrimonio)
				.set("AD_ENVIARPLANOGRAMA",tipo)
				.update();

		} catch (Exception e) {
			String erro = "[marcarEnviarPlanogramaNaTelaInstalacao] Nao foi possivel gravar o campo enviar planograma! \n" + e.getMessage() + "\n"+ e.getCause();
			salvarException(erro);
		}
	}
	
	private void chamaPentahoPlanograma() {

		try {

			String site = "http://pentaho.grancoffee.com.br:8080/pentaho/kettle/";
			String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC/Projetos/GCW/Transformations/";
			String objName = "TF - GSN009 - Enviar Planograma";
			String objName2 = "TF - GSN009 - Enviar Planograma Loja Uppay";

			si.runTrans(path, objName);
			si.runTrans(path, objName2);

		} catch (Exception e) {
			salvarException("[chamaPentahoPlanograma] Não foi possível chamar a Rotina Pentaho de Cadastro!" + e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void salvarException(String mensagem) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("OBJETO", "evento_cadastrarLojaUppay");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);
			
			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
			
		} catch (Exception e) {
			//aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! "+e.getMessage());
		}
	}
	
}
