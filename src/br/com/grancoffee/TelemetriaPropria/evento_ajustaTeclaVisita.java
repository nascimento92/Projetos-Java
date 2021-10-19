package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class evento_ajustaTeclaVisita implements EventoProgramavelJava {

	/**
	 * 17/10/2021 vs 1.2 inserido as validações necessárias para atualizar os dados na AD_TROCADEGRADE
	 */
	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		
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
		delete(arg0);
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		start(arg0);
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		update(arg0);
	}
	
	private void delete(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();	
		BigDecimal numos = VO.asBigDecimal("NUMOS");
		BigDecimal produto = VO.asBigDecimal("CODPROD");
		String tecla = VO.asString("TECLA");
		
		atualizaQtdContada(numos,produto,tecla,new BigDecimal(0),null,null);
	}
	
	private void update(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();	
		BigDecimal numos = VO.asBigDecimal("NUMOS");
		BigDecimal produto = VO.asBigDecimal("CODPROD");
		BigDecimal qtdcontada = VO.asBigDecimal("QTDCONTAGEM");
		String tecla = VO.asString("TECLA");
		String molavazia = VO.asString("MOLAVAZIA");
		Timestamp data = VO.asTimestamp("DTVALIDADE");
		
		atualizaQtdContada(numos,produto,tecla,qtdcontada,molavazia,data);
		
		VO.setProperty("DTALTER", TimeUtils.getNow());
	}

	private void start(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		String tecla = VO.asString("TECLA");
		BigDecimal numos = VO.asBigDecimal("NUMOS");
		BigDecimal produto = VO.asBigDecimal("CODPROD");
		BigDecimal qtdcontada = VO.asBigDecimal("QTDCONTAGEM");
		String molavazia = VO.asString("MOLAVAZIA");
		Timestamp data = VO.asTimestamp("DTVALIDADE");
		
		String obtemTecla = "";

		if (tecla == null) {
			
			obtemTecla = obtemTecla(numos, produto);
			
			if(obtemTecla==null) {
				obtemTecla = "0";
			}
			
			VO.setProperty("TECLA", obtemTecla);
		}else {
			obtemTecla = tecla;
		}
		
		atualizaQtdContada(numos,produto,obtemTecla,qtdcontada,molavazia,data);
		
		VO.setProperty("DTREGISTRO", TimeUtils.getNow());
	}
	
	private void atualizaQtdContada(BigDecimal numos, BigDecimal produto, String tecla, BigDecimal qtdcontada, String molavazia, Timestamp data) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_TROCADEGRADE",
					"this.NUMOS=? AND this.CODPROD=? AND this.TECLA=? ", new Object[] { numos, produto,tecla }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("QTDCONTAGEM", qtdcontada);
				
				if(molavazia!=null) {
					VO.setProperty("MOLAVAZIA", molavazia);
				}
				
				if(data!=null) {
					VO.setProperty("DTVALIDADE", data);
				}

				itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			salvarException("[atualizaQtdContada] Nao foi possivel atualizar a contagem! OS: "+numos+" produto: "+produto+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}

	private String obtemTecla(BigDecimal os, BigDecimal produto) {
		
		String tecla = null;
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT T.TECLA FROM GC_PLANOGRAMA T WHERE T.CODBEM = (SELECT CODBEM FROM GC_SOLICITABAST WHERE NUMOS="+os+") AND T.CODPROD = "+produto+" AND ROWNUM=1");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				tecla = contagem.getString("TECLA");
			}
		} catch (Exception e) {
			salvarException("[obtemTecla] Nao foi possivel obter a tecla! OS: "+os+" produto: "+produto+"\n"+e.getMessage()+"\n"+e.getCause());
		}
		
		return tecla;
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "evento_ajustaTeclaVisita");
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
