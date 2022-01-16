package br.com.gsn.Projetos;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.StringUtils;

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
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class evento_calculaTempoAtividades implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		atualizaHoras(arg0);
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		atualizaHoras(arg0);
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
		start(arg0);
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		start(arg0);
	}

	private void start(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		Timestamp dtinicio = VO.asTimestamp("INICIO");
		Timestamp dtfim = VO.asTimestamp("FIM");
		
		if (dtfim != null) {
			double diferencaEmHoras = diferencaEmHoras(dtinicio, dtfim);
			VO.setProperty("TEMPO", new BigDecimal(diferencaEmHoras));
		}
	}
	
	private void atualizaHoras(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal idEtapa = VO.asBigDecimal("IDETAPA");
		BigDecimal idProjeto = VO.asBigDecimal("ID");
		
		BigDecimal tempo = getTempo(idEtapa, idProjeto);
		if (tempo != null) {
			atualizaValor(idProjeto,idEtapa,tempo);
		}
	}

	private void atualizaValor(BigDecimal idprojeto, BigDecimal idetapa, BigDecimal horas) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_REQETAPAS",
					"this.ID=? AND this.IDETAPA=? ", new Object[] { idprojeto, idetapa }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("HRSATENDIMENTO", horas);

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private BigDecimal getTempo(BigDecimal etapa, BigDecimal projeto) throws Exception {
		BigDecimal horas = BigDecimal.ZERO;

		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql(
				"SELECT SUM(TEMPO) AS TEMPO FROM AD_REQATIVIDADE WHERE IDETAPA=" + etapa + " AND ID=" + projeto);
		contagem = nativeSql.executeQuery();
		while (contagem.next()) {
			BigDecimal count = contagem.getBigDecimal("TEMPO");
			if (count != null) {
				horas = count;
			}
		}

		return horas;
	}

	public double diferencaEmHoras(Timestamp dataInicio, Timestamp dataFim) {

		String dataInicioApenasHoras = StringUtils.formatTimestamp(dataInicio, "HH:mm:ss");
		String dataFimApenasHoras = StringUtils.formatTimestamp(dataFim, "HH:mm:ss");

		String[] horaEntradaHMS = dataInicioApenasHoras.split(":");
		String[] horaSaidaHMS = dataFimApenasHoras.split(":");
		Calendar calHorasTrabalhadas = Calendar.getInstance();

		// Set hora saida
		calHorasTrabalhadas.set(Calendar.HOUR, Integer.parseInt(horaSaidaHMS[0]));
		calHorasTrabalhadas.set(Calendar.MINUTE, Integer.parseInt(horaSaidaHMS[1]));
		calHorasTrabalhadas.set(Calendar.SECOND, Integer.parseInt(horaSaidaHMS[2]));

		// Subtrai as horas entrada
		calHorasTrabalhadas.add(Calendar.HOUR, (-1) * Integer.parseInt(horaEntradaHMS[0]));
		calHorasTrabalhadas.add(Calendar.MINUTE, (-1) * Integer.parseInt(horaEntradaHMS[1]));
		calHorasTrabalhadas.add(Calendar.SECOND, (-1) * Integer.parseInt(horaEntradaHMS[2]));

		double segundos = calHorasTrabalhadas.get(Calendar.SECOND);
		double minutos = calHorasTrabalhadas.get(Calendar.MINUTE);
		double horas = calHorasTrabalhadas.get(Calendar.HOUR);

		double tempoCorreto = ((segundos / 60) / 60) + (minutos / 60) + horas;
		double horaFinal = converterDoubleDoisDecimais(tempoCorreto);

		return horaFinal;
	}

	public double converterDoubleDoisDecimais(double precoDouble) {
		DecimalFormat fmt = new DecimalFormat("0.00");
		String string = fmt.format(precoDouble);
		String[] part = string.split("[,]");
		String string2 = part[0] + "." + part[1];
		double preco = Double.parseDouble(string2);
		return preco;
	}

}
