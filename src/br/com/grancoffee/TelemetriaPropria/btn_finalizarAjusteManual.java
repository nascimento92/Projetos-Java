package br.com.grancoffee.TelemetriaPropria;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Timer;

import com.sankhya.util.TimeUtils;

import Helpers.WSPentaho;
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

public class btn_finalizarAjusteManual implements AcaoRotinaJava {

	private int qtd = 0;
	private int zerados = 0;
	private int preenchidos = 0;

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {

		boolean confirmarSimNao = arg0.confirmarSimNao("ATENÇÃO!", "O sistema agendará o ajuste, continuar?", 1);
		if (confirmarSimNao) {
			start(arg0);
			arg0.setMensagemRetorno("Agendamento concluido</b>!");
		}
	}

	private void start(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		Object id = linhas[0].getCampo("ID");
		Object patrimonio = linhas[0].getCampo("CODBEM");
		Object ajustado = linhas[0].getCampo("AJUSTADO");

		if ("S".equals(ajustado)) {
			arg0.mostraErro("<b>Erro! Teclas já ajustadas</b>");
		} else {
			salvarTeclas(id, patrimonio);
			gravaValor(id);
			
			Timer timer = new Timer(10000, new ActionListener() {	
				@Override
				public void actionPerformed(ActionEvent e) {
					chamaPentaho();				
				}
			});
			timer.setRepeats(false);
			timer.start();
		}
	}

	private void salvarTeclas(Object id, Object patrimonio) {
		try {

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_ITENSAJUSTESMANUAIS",
					"this.ID=? AND this.CODBEM=? ", new Object[] { id, patrimonio }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);

				if (DynamicVO.asBigDecimal("SALDOAPOS") == null) {
					DynamicVO.setProperty("QTDAJUSTE", new BigDecimal(0));
					itemEntity.setValueObject((EntityVO) DynamicVO);
				}

				if (DynamicVO.asBigDecimal("QTDAJUSTE").intValue() != 0) {

					inserirSolicitacaoDeAjuste(DynamicVO.asString("CODBEM"), DynamicVO.asString("TECLA"),
							DynamicVO.asBigDecimal("CODPROD"), DynamicVO.asBigDecimal("CAPACIDADE"),
							DynamicVO.asBigDecimal("NIVELPAR"), DynamicVO.asBigDecimal("SALDOANTES"),
							DynamicVO.asBigDecimal("QTDAJUSTE"), id, DynamicVO.asString("MOTIVO"));

				}

				DynamicVO.setProperty("AJUSTADO", "S");
				itemEntity.setValueObject((EntityVO) DynamicVO);
			}

		} catch (Exception e) {
			salvarException("Não foi possivel puxar as teclas!" + e.getMessage() + "\n" + e.getCause());
		}
	}

	private void inserirSolicitacaoDeAjuste(String codbem, String tecla, BigDecimal produto, BigDecimal capacidade,
			BigDecimal nivelpar, BigDecimal saldoAtual, BigDecimal diferenca, Object idObjeto, String motivo) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCSolicitAjuste");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("CODBEM", codbem);
			VO.setProperty("CODUSU", getUsuLogado());
			VO.setProperty("TECLA", tecla);
			VO.setProperty("CODPROD", produto);
			VO.setProperty("CAPACIDADE", capacidade);
			VO.setProperty("NIVELPAR", nivelpar);
			VO.setProperty("SALDOANTERIOR", saldoAtual);
			VO.setProperty("QTDAJUSTE", diferenca);
			VO.setProperty("MANUAL", "S");
			VO.setProperty("SALDOFINAL", saldoAtual.add(diferenca));
			VO.setProperty("IDABASTECIMENTO", idObjeto);
			VO.setProperty("AD_DTSOLICIT", TimeUtils.getNow());

			if (motivo != null) {
				VO.setProperty("OBSERVACAO", motivo);
			} else {
				VO.setProperty("OBSERVACAO", "Ajuste Manual");
			}

			dwfFacade.createEntity("GCSolicitAjuste", (EntityVO) VO);

		} catch (Exception e) {
			salvarException("Não foi possivel cadastrar a solicitação de ajuste na tela instalações!" + e.getMessage()
					+ "\n" + e.getCause());
		}
	}
	
	private void gravaValor(Object id) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			PersistentLocalEntity PersistentLocalEntity = dwfFacade.findEntityByPrimaryKey("AD_AJUSTESMANUAIS", id);
			EntityVO NVO = PersistentLocalEntity.getValueObject();
			DynamicVO appVO = (DynamicVO) NVO;
			
			verificaCalculos(id);
			
			if(this.zerados==this.qtd) {
				appVO.setProperty("MAQUINAZERADA","S");
			}
			
			if(this.preenchidos==this.qtd) {
				appVO.setProperty("MAQUINAPREENCHIDA","S");
			}
			
			PersistentLocalEntity.setValueObject(NVO);
		} catch (Exception e) {
			salvarException("Não foi possivel salvar os valores na tela Ajuste Manual!" + e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void verificaCalculos(Object id) {
		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"select distinct\r\n" + 
					"t.id,\r\n" + 
					"(select count(*) from AD_ITENSAJUSTESMANUAIS where id=t.id) as qtd,\r\n" + 
					"(select count(*) from AD_ITENSAJUSTESMANUAIS where saldoapos=0 and id=t.id) as zerados,\r\n" + 
					"(select count(*) from AD_ITENSAJUSTESMANUAIS where saldoapos=nivelpar and id=t.id) as preenchidos\r\n" + 
					"from AD_ITENSAJUSTESMANUAIS t\r\n" + 
					"where id="+id);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				this.qtd = contagem.getInt("qtd");
				this.zerados = contagem.getInt("zerados");
				this.preenchidos = contagem.getInt("preenchidos");
			}

		} catch (Exception e) {
			salvarException("Não foi possivel validar os calculos!" + e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void chamaPentaho() {
		
		try {
			
			String site = "http://pentaho.grancoffee.com.br:8080/pentaho/kettle/";
		    String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
		    WSPentaho si = new WSPentaho(site, Key);
		    		    
		    String path = "home/GC/Projetos/GCW/Transformations/";
		    String objName = "TF - GSN007 - Verifica Solicitacoes de ajuste";
		    String objName2 = "TF - GSN002 - Salva Estoque MID-SKW";
		    
		    si.runTrans(path, objName);
		    si.runTrans(path, objName2);
			
		} catch (Exception e) {
			salvarException("Não foi possível chamar a Rotina Pentaho!" + e.getMessage()+"\n"+e.getCause());
		}		
	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
		codUsuLogado = ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID();
		return codUsuLogado;
	}

	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "btn_zerarMaquina");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}

}
