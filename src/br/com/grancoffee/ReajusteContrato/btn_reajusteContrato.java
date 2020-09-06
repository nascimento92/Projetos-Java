package br.com.grancoffee.ReajusteContrato;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btn_reajusteContrato implements AcaoRotinaJava {
	
	String retornoPositivo="";
	String retornoNegativo="";
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		boolean confirmarSimNao = arg0.confirmarSimNao("Atenção", "Reajustar Contratos", 1);
		
		if(confirmarSimNao) {
			Registro[] linhas = arg0.getLinhas();
			start(linhas,arg0);
		}
		
		arg0.setMensagemRetorno("<b>Contratos reajustados:</b> "+retornoPositivo+"\n<br/><b>Contratos com erros:</b> "+retornoNegativo);
	}
	
	private void start(Registro[] linhas,ContextoAcao arg0) throws Exception {
		
		Timestamp dtBaseReajuste = (Timestamp) arg0.getParam("DTBASEREAJ");
		Double porcentagem = (Double) arg0.getParam("PORCENTAGEM");
		String observacao = (String) arg0.getParam("OBSERVACAO");
		
		for(int i=0; i<linhas.length; i++) {
			BigDecimal contrato = (BigDecimal) linhas[i].getCampo("NUMCONTRATO");
			
			if(cadastrarDados(contrato,porcentagem,dtBaseReajuste,observacao)) {

				alterarDataBaseReajuste(contrato, dtBaseReajuste);
				retornoPositivo = retornoPositivo + contrato + ", ";

			}	
		}
	}
		
	private void alterarDataBaseReajuste(BigDecimal contrato, Timestamp dtbase) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			PersistentLocalEntity PersistentLocalEntity = dwfFacade.findEntityByPrimaryKey("Contrato", contrato);
			EntityVO NVO = PersistentLocalEntity.getValueObject();
			DynamicVO appVO = (DynamicVO) NVO;

			appVO.setProperty("DTBASEREAJ", dtbase);

			PersistentLocalEntity.setValueObject(NVO);

			
		} catch (Exception e) {
			retornoNegativo=retornoNegativo+"\nCT: "+contrato+" motivo: Erro ao ajustar Data base reajuste";
			e.getMessage();
			e.printStackTrace();
		}
	}
	
	private boolean cadastrarDados(BigDecimal contrato,Double porcentagem,Timestamp data,String obs) throws Exception {
		boolean cadastroPre=false;
		double valorFinal=0;
		
		String ultimaReferencia = verificaUltimaReferencia(contrato);
		
		int compareTo = "01/01/0001".compareTo(ultimaReferencia);
				
		if(compareTo!=0) {

			Double valor = verificaultimoValorDaReferencia(ultimaReferencia,contrato);
			valorFinal=valor+(valor*(porcentagem/100));
					
			try { //insert TCSPRE
				
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("PrecoContrato");
				DynamicVO VO = (DynamicVO) NPVO;
				
				VO.setProperty("NUMCONTRATO", contrato);
				VO.setProperty("CODPROD", new BigDecimal(8));
				VO.setProperty("REFERENCIA", new Timestamp(System.currentTimeMillis()));
				VO.setProperty("VALOR", new BigDecimal(valorFinal).setScale(2, BigDecimal.ROUND_HALF_EVEN));
				VO.setProperty("AD_OBS", "Inserido via botão - Ajustar Contrato pelo usuário: "+getUsuLogado());
				
				dwfFacade.createEntity("PrecoContrato", (EntityVO) VO);
				
				
				EntityVO NPVOS = dwfFacade.getDefaultValueObjectInstance("Reajuste");
				DynamicVO VOS = (DynamicVO) NPVOS;
				
				VOS.setProperty("NUMCONTRATO", contrato);
				VOS.setProperty("PORCREAJUSTE2", new BigDecimal(porcentagem));
				VOS.setProperty("DTREFERENCIA", data);
				VOS.setProperty("CODUSU", getUsuLogado());
				VOS.setProperty("OBSERVACAO", obs.toCharArray());
				
				dwfFacade.createEntity("Reajuste", (EntityVO) VOS);
				
				cadastroPre=true;
				
			} catch (Exception e) {
				e.getMessage();
				e.printStackTrace();
				retornoNegativo = retornoNegativo+contrato+", ";
			}
			
			
		}else {
			retornoNegativo=retornoNegativo+contrato+", ";
		}
		return cadastroPre;
	}
	
	private String verificaUltimaReferencia(BigDecimal contrato) throws Exception {
		
		String referencia = "";
		
		try {
			

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT TO_CHAR(NVL(MAX(REFERENCIA),'01/01/0001'),'DD/MM/YYYY') AS ULTREF FROM TCSPRE WHERE numcontrato="+contrato+" and codprod=8");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				referencia = contagem.getString("ULTREF");
			}
			
		} catch (Exception e) {
			e.getMessage();
			e.printStackTrace();
			retornoNegativo=retornoNegativo+contrato+" sem valor na tabela de preço.";
		}
		
		return referencia;
	}
	
	private Double verificaultimoValorDaReferencia(String referencia, BigDecimal contrato) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PrecoContrato");
		DynamicVO VO = DAO.findOne("REFERENCIA=? AND NUMCONTRATO=?",new Object[] { referencia,contrato });
		Double valor = VO.asDouble("VALOR");
		return valor;
	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
	    codUsuLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
	    return codUsuLogado;    	
	}
	
	/*
	 * private boolean verificaADREAJUSTE(BigDecimal contrato, Double porcentagem,
	 * Timestamp data, String obs) { boolean valida=false;
	 * 
	 * try {
	 * 
	 * EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade(); EntityVO NPVO =
	 * dwfFacade.getDefaultValueObjectInstance("Reajuste"); DynamicVO VO =
	 * (DynamicVO) NPVO;
	 * 
	 * VO.setProperty("NUMCONTRATO", contrato); VO.setProperty("PORCREAJUSTE2", new
	 * BigDecimal(porcentagem)); VO.setProperty("DTREFERENCIA", data);
	 * VO.setProperty("CODUSU", getUsuLogado()); VO.setProperty("OBSERVACAO",
	 * obs.toCharArray());
	 * 
	 * dwfFacade.createEntity("Reajuste", (EntityVO) VO);
	 * 
	 * valida=true;
	 * 
	 * } catch (Exception e) { e.getMessage(); e.printStackTrace(); retornoNegativo
	 * = retornoNegativo+contrato+", "; }
	 * 
	 * return valida; }
	 */
	
	/*
	 * private void deletaTCSPRE(BigDecimal contrato) { try { EntityFacade dwfFacade
	 * = EntityFacadeFactory.getDWFFacade(); dwfFacade.removeByCriteria(new
	 * FinderWrapper("PrecoContrato", "this.NUMCONTRATO=? AND REFERENCIA=?",new
	 * Object[] {contrato, new Timestamp(System.currentTimeMillis())})); } catch
	 * (Exception e) { e.getMessage(); e.printStackTrace(); } }
	 */
}
