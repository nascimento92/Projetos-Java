package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btn_ajusteManual implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		start(arg0);		
	}
	
	private void start(ContextoAcao arg0) throws Exception {
		String patrimonio = (String) arg0.getParam("CODBEM");
		String motivo = (String) arg0.getParam("MOTIVO");
		
		BigDecimal idabast = cadastrarAbastecimento(patrimonio,arg0,motivo);
		
		if(idabast!=null) {		
			carregaTeclasNosItensDeAbast(patrimonio,idabast);
			arg0.setMensagemRetorno("Ajuste Cadastrado, ID: <b>"+idabast+"</b>, inserir as informações e depois clicar no botão <b>Finalizar Ajuste</b>");
		}
	}
	
	private BigDecimal cadastrarAbastecimento(String patrimonio,ContextoAcao arg0,String motivo) {
		BigDecimal idAbastecimento = null;
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCControleAbastecimento");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("DTSOLICITACAO", TimeUtils.getNow());
			VO.setProperty("STATUS", "1");
			VO.setProperty("SOLICITANTE", arg0.getUsuarioLogado());
			VO.setProperty("ROTA", Integer.toString(getRota(patrimonio)));
			VO.setProperty("OBSABAST",motivo.toCharArray());
			VO.setProperty("AJUSTEMANUAL","S");
			
			dwfFacade.createEntity("GCControleAbastecimento", (EntityVO) VO);
			
			idAbastecimento = VO.asBigDecimal("ID");
			
		} catch (Exception e) {
			System.out.println("## [btn_ajusteManual] ## - Não foi possivel cadastrar um ajuste manual!");
			e.getMessage();
			e.getCause();
			e.printStackTrace();
		}
		
		return idAbastecimento;
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
		}
		
		return count;
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
			BigDecimal estoque = DynamicVO.asBigDecimal("ESTOQUE");
			
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
				VO.setProperty("SALDOANTERIOR", estoque);
				VO.setProperty("QTDABAST", new BigDecimal(0));
				VO.setProperty("SALDOABAST", new BigDecimal(0));
				VO.setProperty("DIFERENCA", new BigDecimal(0));
				VO.setProperty("AD_QTDPARAABAST", new BigDecimal(0));
				VO.setProperty("AJUSTADO", "N");
				
				dwfFacade.createEntity("GCItensAbastecimento", (EntityVO) VO);
				
			} catch (Exception e) {
				System.out.println("## [btn_ajusteManual] ## - Não foi possivel cadastrar as teclas");
				e.getMessage();
				e.printStackTrace();
			}
			
		
		}
}

}
