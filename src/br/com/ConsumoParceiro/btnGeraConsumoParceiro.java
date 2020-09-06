package br.com.ConsumoParceiro;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;

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
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btnGeraConsumoParceiro implements AcaoRotinaJava {
	/**
	 * Botão não está funcionando o update.
	 */
	public void doAction(ContextoAcao arg0) throws Exception {
		
		try {
			Registro[] linhas = arg0.getLinhas();
			
			if(linhas.length>1) {
				arg0.mostraErro("Selecione apenas um faturamento para gerar o consumo!");
			}else {
				start(linhas,arg0);
			}
			
		} catch (Exception e) {
			System.out.println("*******---*******Não foi possivel registrar o consumo! "+e.getMessage());
		}
	}
	
	private void start(Registro[] linhas,ContextoAcao arg0) throws Exception {
		BigDecimal nrFaturamento = (BigDecimal) linhas[0].getCampo("NUFATURA");
				
		if (validaSeJaExisteConsumoParaOhFaturamento(nrFaturamento)) {

			delete(nrFaturamento);
			getCampos(nrFaturamento);
			arg0.setMensagemRetorno("Consumo Atualizado!");

		} else {

			getCampos(nrFaturamento);
			arg0.setMensagemRetorno("Consumo Registrado!");
		}
		
	}
	
	private boolean validaSeJaExisteConsumoParaOhFaturamento(BigDecimal nrFaturamento) throws Exception {
		boolean valida = false;
		
		JapeWrapper DAO = JapeFactory.dao("AD_CONSUMOPARCEIRO");
		DynamicVO VO = DAO.findOne("NUFATURAMENTO=?",new Object[] { nrFaturamento });
		
		if(VO!=null) {
			valida=true;
		}

		return valida;
	}
	
	private void getCampos(BigDecimal nrFaturamento) throws Exception {
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT NUMCONTRATO,IDPLANTA,CODMATPRIMA,SUM(QTDCONSUMO) AS QTDCONSUMO, CODVOL FROM AD_CONSUMO WHERE NRO_FATURAMENTO="+nrFaturamento+" group by NUMCONTRATO,IDPLANTA,CODMATPRIMA,CODVOL");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			BigDecimal contrato = contagem.getBigDecimal("NUMCONTRATO");
			BigDecimal idPlanta = contagem.getBigDecimal("IDPLANTA");
			BigDecimal codMateriaPrima = contagem.getBigDecimal("CODMATPRIMA");
			BigDecimal qtdConsumo = contagem.getBigDecimal("QTDCONSUMO");
			String volume = contagem.getString("CODVOL");
			
			insert(nrFaturamento,codMateriaPrima,contrato,idPlanta,qtdConsumo,volume);
			
		}
	}
	
	private void insert(BigDecimal nrFaturamento,BigDecimal codMateriaPrima,BigDecimal contrato,BigDecimal idPlanta,BigDecimal qtdConsumo,String volume) throws Exception {
		
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_CONSUMOPARCEIRO");
		DynamicVO VO = (DynamicVO) NPVO;
		
		BigDecimal periodo = new BigDecimal(30);
		
		VO.setProperty("NUFATURAMENTO", nrFaturamento);
		VO.setProperty("CODMATPRIMA", codMateriaPrima);
		VO.setProperty("NUMCONTRATO", contrato);
		VO.setProperty("IDPLANTA", idPlanta);
		VO.setProperty("QTDCONSUMO", qtdConsumo);
		VO.setProperty("CODVOL", volume);
		VO.setProperty("PERIODO", periodo);
		
		if("GR".equals(volume)) {
			BigDecimal emkg = qtdConsumo.divide(new BigDecimal(1000));
			VO.setProperty("EMKG", emkg);
		}
	 
		BigDecimal consumoDiario = qtdConsumo.divide(periodo, 2, RoundingMode.HALF_UP);
		VO.setProperty("CONSUMODIARIO", consumoDiario);

		BigDecimal codlocal = getLocal(contrato, idPlanta);
		
		if (codlocal != null) {
			BigDecimal frequencia = getFrequencia(codlocal);
			if (frequencia != null) {
				VO.setProperty("FREQUENCIA", frequencia);

				BigDecimal enviar = consumoDiario.multiply(frequencia);
				
				if("GR".equals(volume)) {
					BigDecimal enviarKg = enviar.divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);
					VO.setProperty("ENVIAR", enviarKg);
					VO.setProperty("UNIDENVIO", "KG");
				}else {
					BigDecimal enviarArredondado = enviar.setScale(0,RoundingMode.CEILING);
					VO.setProperty("ENVIAR", enviarArredondado);
						if("UN".equals(volume)) {
							VO.setProperty("UNIDENVIO", "UN");
						}
				}
				
			}
		}
		
		
		dwfFacade.createEntity("AD_CONSUMOPARCEIRO", (EntityVO) VO);
	}
	
	private BigDecimal getLocal(BigDecimal numcontrato, BigDecimal idPlanta) throws Exception {
		BigDecimal codlocal = null;
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("PLANTAS","this.NUMCONTRATO=? AND this.ID=? ", new Object[] { numcontrato, idPlanta }));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

		PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
		DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			if (DynamicVO != null) {
				codlocal = DynamicVO.asBigDecimal("CODLOCAL");
			}
		}
		return codlocal;
	}
	
	private BigDecimal getFrequencia(BigDecimal codlocal) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("LocalFinanceiro");
		DynamicVO VO = DAO.findOne("CODLOCAL=?",new Object[] { codlocal });
		
		String frequencia = VO.asString("AD_FREQREPO");
		BigDecimal frequenciaDias = null;
		
		if(frequencia!=null) {
			if("1".equals(frequencia)) {
				frequenciaDias = new BigDecimal(7);
			}
			else if("2".equals(frequencia)) {
				frequenciaDias = new BigDecimal(15);
			}
			else if("3".equals(frequencia)) {
				frequenciaDias = new BigDecimal(30);
			}
			else if("4".equals(frequencia)) {
				frequenciaDias = new BigDecimal(60);
			}
			else if("5".equals(frequencia)) {
				frequenciaDias = new BigDecimal(3);
			}
			else if("2".equals(frequencia)) {
				frequenciaDias = new BigDecimal(2);
			}
		}
		
		return frequenciaDias;
	}
	
	private void delete(BigDecimal nrFaturamento) throws Exception {
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		dwfFacade.removeByCriteria(new FinderWrapper("AD_CONSUMOPARCEIRO","this.nufaturamento = ? ", new Object[] { nrFaturamento }));
		
	}
}
