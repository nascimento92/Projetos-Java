package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btn_abastecimento implements AcaoRotinaJava{
	String retornoNegativo="";
	int cont=0;
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		
		start(linhas,arg0);
	}
	
	private void start(Registro[] linhas,ContextoAcao arg0) throws Exception {
		
		String tipoAbastecimento = (String) arg0.getParam("TIPABAST");
		Timestamp dtAbastecimento = (Timestamp) arg0.getParam("DTABAST");
		Timestamp dtSolicitacao;
		
		if("2".equals(tipoAbastecimento) && dtAbastecimento==null) {
			arg0.mostraErro("<b>ERRO!</b> - Pedidos agendados precisam de uma data de abastecimento!");
		}
		
		if("1".equals(tipoAbastecimento) && dtAbastecimento!=null) {
			dtAbastecimento = null;
		}
		
		if("2".equals(tipoAbastecimento) && dtAbastecimento!=null) {
			dtSolicitacao = TimeUtils.getNow();
			if(dtAbastecimento.before(reduzUmDia(dtSolicitacao))) {
				arg0.mostraErro("<b>ERRO!</b> - Data de agendamento não pode ser menor que a data de hoje!");
			}
			
			int diaDataAgendada = TimeUtils.getDay(dtAbastecimento);
			int diaDataAtual = TimeUtils.getDay(dtSolicitacao);
			
			int minutoDataAgendada = TimeUtils.getTimeInMinutes(dtAbastecimento);
			int minutoDataAtual = TimeUtils.getTimeInMinutes(dtSolicitacao);
			
			if(diaDataAtual==diaDataAgendada) {
				if(minutoDataAtual==minutoDataAgendada) {
					dtAbastecimento = null;
				}
			}
		}
		
		for(int i=0; i<linhas.length; i++) {
			if(!validaPedido(linhas[i].getCampo("CODBEM").toString())) {
				cont++;
				
				if(dtAbastecimento!=null) {
					dtSolicitacao = TimeUtils.getNow();
					BigDecimal idAbastecimento = cadastrarNovoAbastecimento(linhas[i].getCampo("CODBEM").toString(),dtSolicitacao);
					
					if(idAbastecimento!=null) {
						carregaTeclasNosItensDeAbast(linhas[i].getCampo("CODBEM").toString(),idAbastecimento);
						agendarAbastecimento(linhas[i].getCampo("CODBEM").toString(),dtSolicitacao,dtAbastecimento,idAbastecimento);
					}else {
						cont=0;
						retornoNegativo = retornoNegativo+linhas[i].getCampo("CODBEM").toString();
					}
					
					
				}else {
					dtSolicitacao = TimeUtils.getNow();
					BigDecimal idAbastecimento = cadastrarNovoAbastecimento(linhas[i].getCampo("CODBEM").toString(),dtSolicitacao);
					
					if(idAbastecimento!=null) {
						carregaTeclasNosItensDeAbast(linhas[i].getCampo("CODBEM").toString(),idAbastecimento);
						agendarAbastecimento(linhas[i].getCampo("CODBEM").toString(),dtSolicitacao,dtSolicitacao,idAbastecimento);
					}else {
						cont=0;
						retornoNegativo = retornoNegativo+linhas[i].getCampo("CODBEM").toString();
					}
					
				}
								
			}
		}
		
		if(retornoNegativo!="") {
			arg0.setMensagemRetorno("<b>Erro</b> Patrimônios com pedidos pendentes: "+retornoNegativo);
		}else {
			arg0.setMensagemRetorno(cont+" - Abastecimento(s) Agendado(s)");
		}
	}
	
	private BigDecimal cadastrarNovoAbastecimento(String patrimonio, Timestamp data) {
		BigDecimal idAbastecimento = null;
		
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCControleAbastecimento");
			DynamicVO VO = (DynamicVO) NPVO;
			
			int rota = getRota(patrimonio);
			
			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("DTSOLICITACAO", data);
			VO.setProperty("STATUS", "1");
			VO.setProperty("SOLICITANTE", getUsuLogado());
			VO.setProperty("ROTA", Integer.toString(rota));
			
			dwfFacade.createEntity("GCControleAbastecimento", (EntityVO) VO);
			
			idAbastecimento = VO.asBigDecimal("ID");
			
		} catch (Exception e) {
			retornoNegativo = retornoNegativo+e.getMessage();
			e.getMessage();
			e.printStackTrace();
		}
		
		return idAbastecimento;
	}
	
	private boolean validaPedido(String patrimonio) {
		boolean valida=false;
		
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT COUNT(*) FROM GC_SOLICITABAST WHERE CODBEM='"+patrimonio+"' AND STATUS IN ('1','2')");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				if (count >= 1) {
					valida = true;
					retornoNegativo=retornoNegativo+patrimonio+", ";
				}
			}
			
		} catch (Exception e) {
			e.getMessage();
			e.printStackTrace();
			retornoNegativo=retornoNegativo+patrimonio+", ";
		}
		
		return valida;
	}
	
	private void agendarAbastecimento(String patrimonio, Timestamp dtSolicitacao, Timestamp dtAgendamento,BigDecimal idAbastecimento) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCSolicitacoesAbastecimento");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("CODUSU", getUsuLogado());
			VO.setProperty("STATUS", "1");
			VO.setProperty("DTSOLICIT", dtSolicitacao);
			VO.setProperty("DTAGENDAMENTO", dtAgendamento);
			VO.setProperty("ROTA", new BigDecimal(getRota(patrimonio)));
			VO.setProperty("IDABASTECIMENTO", idAbastecimento);
			
			dwfFacade.createEntity("GCSolicitacoesAbastecimento", (EntityVO) VO);
			
		} catch (Exception e) {
			e.getMessage();
			e.getCause();
			e.printStackTrace();
		}
	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
		codUsuLogado = ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID();
		return codUsuLogado;
	}
	
	private int getRota(String patrimonio) {
		int count=0;
		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT ID FROM AD_ROTATEL WHERE ID IN (SELECT ID FROM AD_ROTATELINS WHERE codbem='"+patrimonio+"') AND auditoria='S'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				count = contagem.getInt("ID");
			}

		} catch (Exception e) {
			e.getMessage();
			e.printStackTrace();
			retornoNegativo = retornoNegativo + patrimonio + ", ";
		}
		
		return count;
	}
	
	private Timestamp reduzUmDia(Timestamp data) {
		Calendar dataAtual = Calendar.getInstance();
		dataAtual.setTime(data);
		dataAtual.add(Calendar.DAY_OF_MONTH, -1);
		return new Timestamp(dataAtual.getTimeInMillis());
	}
	
	private void carregaTeclasNosItensDeAbast(String patrimonio, BigDecimal idAbastecimento) throws Exception {
		
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("GCPlanograma", "this.CODBEM = ? ", new Object[] { patrimonio }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);

				String tecla = DynamicVO.asString("TECLA");
				BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
				BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
				BigDecimal nivelPar = DynamicVO.asBigDecimal("NIVELPAR");
				
				try {
					
					EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
					EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCItensAbastecimento");
					DynamicVO VO = (DynamicVO) NPVO;
					
					VO.setProperty("IDABASTECIMENTO", idAbastecimento);
					VO.setProperty("CODBEM", patrimonio);
					VO.setProperty("TECLA", tecla);
					VO.setProperty("CODPROD", produto);
					VO.setProperty("CAPACIDADE", capacidade);
					VO.setProperty("NIVELPAR", nivelPar);
					
					dwfFacade.createEntity("GCItensAbastecimento", (EntityVO) VO);
					
				} catch (Exception e) {
					System.out.println("***** NAO FOI POSSIVEL CADASTRAR AS TECLAS *******");
					e.getMessage();
					e.printStackTrace();
				}
				
			
			}
	}
}
