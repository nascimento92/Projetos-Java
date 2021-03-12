package br.com.grancoffee.TelemetriaPropria;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.ResultSet;

import javax.swing.Timer;

import com.sankhya.util.TimeUtils;
import Helpers.WSPentaho;
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
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
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.ws.ServiceContext;

public class evento_validaTelPrincipal implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		afterInsertUpdate(arg0);
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		afterInsertUpdate(arg0);
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
		validacoes(arg0);
		/*
		String patrimonio = VO.asString("CODBEM");
		
		validacoes(arg0);
		
		if("S".equals(VO.asString("PRINCIPAL"))) {

			int qtd = verificaSeJaExisteUmaTelemetriaPrincipal(patrimonio);
			
			if(qtd>0) {
				throw new PersistenceException("<br/><br/><br/><b>Erro - Já existe uma telemetria como principal!</b><br/><br/><br/>");
			}
		}	
		*/
		VO.setProperty("AD_INTEGRADO", "N");
		verificaSeEhUmaLojaUppay(VO);
	}
	
	private void afterInsertUpdate(PersistenceEvent arg0) {
		cadastrarPatrimonioMID();
	}
		
	private void update(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		validacoes(arg0);
		/*
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();	
		String patrimonio = VO.asString("CODBEM");
		
		validacoes(arg0);
		
		if("N".equals(oldVO.asString("PRINCIPAL")) || oldVO.asString("PRINCIPAL")==null) {
			if("S".equals(VO.asString("PRINCIPAL"))) {

				int qtd = verificaSeJaExisteUmaTelemetriaPrincipal(patrimonio);
				
				if(qtd>0) {
					throw new PersistenceException("<br/><br/><br/><b>Erro - Já existe uma telemetria como principal!</b><br/><br/><br/>");
				}
			}
		}	
		*/
		VO.setProperty("AD_INTEGRADO", "N");
		verificaSeEhUmaLojaUppay(VO);
	}
	
	private void validacoes(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		String patrimonio = VO.asString("CODBEM");
		BigDecimal idtel = VO.asBigDecimal("IDTEL");
		String pinpadDigital = VO.asString("AD_PINPADDIG");
		String boxVerti = VO.asString("AD_BOXVERTI");
		
		if(idtel.intValue()==1 && pinpadDigital!=null) {
			throw new Error("<br/><br/><br/><b> Pinpad Digital não pode ser vinculado a Verti! <br/><br/><br/><b>");
		}
		
		if(idtel.intValue()==2 && boxVerti!=null) {
			throw new Error("<br/><br/><br/><b> Box não pode ser vinculada a Uppay! <br/><br/><br/><b>");
		}
		
		if(validaSeOhEquipamentoJaEstaVinculado(pinpadDigital,boxVerti,patrimonio)) {
			throw new Error("<br/><br/><br/><b> Equipamento já vinculado em outro Patrimônio! <br/><br/><br/><b>");
		}
	}
	
	private boolean validaSeOhEquipamentoJaEstaVinculado(String pinpadDigital, String boxVerti, String codbem) {
		boolean valida=false;
		
		if(pinpadDigital!=null) {
			try {
				JdbcWrapper jdbcWrapper = null;
				EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
				jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
				ResultSet contagem;
				NativeSql nativeSql = new NativeSql(jdbcWrapper);
				nativeSql.resetSqlBuf();
				nativeSql.appendSql("SELECT COUNT(*) FROM GC_TELINST WHERE AD_PINPADDIG="+pinpadDigital+" AND CODBEM NOT IN('"+codbem+"')");
				contagem = nativeSql.executeQuery();
				while (contagem.next()) {
					int count = contagem.getInt("COUNT(*)");
					if (count >= 1) {
						valida = true;
					}
		}
			} catch (Exception e) {
				salvarException("[validaSeOhEquipamentoJaEstaVinculado] Nao foi possivel validar o equipamento!"+e.getMessage()+"\n"+e.getCause());
			}
		}
		
		if(boxVerti!=null) {
			try {
				JdbcWrapper jdbcWrapper = null;
				EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
				jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
				ResultSet contagem;
				NativeSql nativeSql = new NativeSql(jdbcWrapper);
				nativeSql.resetSqlBuf();
				nativeSql.appendSql("SELECT COUNT(*) FROM GC_TELINST WHERE AD_BOXVERTI="+boxVerti+" AND CODBEM NOT IN('"+codbem+"')");
				contagem = nativeSql.executeQuery();
				while (contagem.next()) {
					int count = contagem.getInt("COUNT(*)");
					if (count >= 1) {
						valida = true;
					}
		}
			} catch (Exception e) {
				salvarException("[validaSeOhEquipamentoJaEstaVinculado] Nao foi possivel validar o equipamento!"+e.getMessage()+"\n"+e.getCause());
			}
		}
		
		return valida;
	}
	
	private int verificaSeJaExisteUmaTelemetriaPrincipal(String patrimonio) throws Exception {
		int qtd = 0;

		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT COUNT(*) AS QTD FROM GC_TELINST WHERE CODBEM='"+patrimonio+"' AND PRINCIPAL='S'");
		contagem = nativeSql.executeQuery();
		while (contagem.next()) {
			qtd = contagem.getInt("QTD");
		}
		
		return qtd;
	}
	
	private void verificaSeEhUmaLojaUppay(DynamicVO VO) throws Exception {
		BigDecimal idTel = VO.asBigDecimal("IDTEL");
		//String principal = VO.asString("PRINCIPAL");
		String patrimonio = VO.asString("CODBEM");
		DynamicVO gcInstalacao = getGcInstalacao(patrimonio);
		String micromarketing = gcInstalacao.asString("TOTEM");
		
		if(idTel.intValue()==2 && "S".equals(micromarketing)) {
			
			Timer timer = new Timer(5000, new ActionListener() {	
				@Override
				public void actionPerformed(ActionEvent e) {
					cadastrarLojaUppay();				
				}
			});
			timer.setRepeats(false);
			timer.start();
		}
	}
	
	private DynamicVO getGcInstalacao(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("GCInstalacao");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		return VO;
	}
	
	private void cadastrarLojaUppay() {
		
		try {
			final String parameter = (String) MGECoreParameter.getParameter("PENTAHOIP");
			String site = parameter;
			//String site = "http://pentaho.grancoffee.com.br:8080/pentaho/kettle/";
		    String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
		    WSPentaho si = new WSPentaho(site, Key);
		    		    
		    String path = "home/GC/Projetos/GCW/Transformations/";
		    String objName = "TF - GSN009 - Criar Loja uppay";
		    
		    si.runTrans(path, objName);
		    		
		} catch (Exception e) {
			salvarException("[cadastrarLojaUppay] Não foi possível chamar a Rotina Pentaho de Cadastro!" + e.getMessage()+"\n"+e.getCause());
		}		
	}
	
	private void cadastrarPatrimonioMID() {

		try {
			final String parameter = (String) MGECoreParameter.getParameter("PENTAHOIP");
			String site = parameter;
			String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC/Projetos/GCW/Jobs/";
			String objName = "JOB - GSN005 - Cadastra patrimonio no MID";

			si.runJob(path, objName);

		} catch (Exception e) {
			salvarException("[chamaPentaho] Nao foi possivel chamar o pentaho! "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void salvarException(String mensagem) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("OBJETO", "evento_validaTelPrincipal");
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
