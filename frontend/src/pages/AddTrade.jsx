// TICKET-ADV123 — React Hook Form + Yup validation.
import React from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { withAuth } from '@components/withAuth.jsx';
import PageTopbar from '@components/PageTopbar.jsx';
import { api } from '@services/apiService.js';

const schema = yup.object({
  tradeRef: yup.string().matches(/^[A-Z]{3}-\d{8}-\d{4}$/, 'Must match AAA-YYYYMMDD-NNNN').required(),
  instrumentId: yup.number().integer().positive().required(),
  counterpartyId: yup.number().integer().positive().required(),
  assetClass: yup.string().oneOf(['EQUITY', 'FIXED_INCOME', 'FX', 'COMMODITY', 'DERIVATIVE']).required(),
  side: yup.string().oneOf(['BUY', 'SELL']).required(),
  quantity: yup.number().positive().required(),
  price: yup.number().positive().required(),
  tradeDate: yup.date().required(),
});

function AddTrade() {
  const { register, handleSubmit, formState: { errors, isSubmitting }, reset, setError } =
        useForm({ resolver: yupResolver(schema) });

  async function onSubmit(values) {
    try {
      await api.createTrade(values);
      reset();
    } catch (err) {
      setError('root', { message: err.message });
    }
  }

  return (
    <section>
      <PageTopbar title="Add Trade" subtitle="Submit a new trade into the blotter." />
      <form onSubmit={handleSubmit(onSubmit)} className="trade-form">
        <label>Trade ref   <input {...register('tradeRef')} placeholder="EQU-20260603-0001" /></label>
        {errors.tradeRef && <p className="form-error" role="alert">{errors.tradeRef.message}</p>}

        <label>Instrument ID <input {...register('instrumentId')} type="number" /></label>
        {errors.instrumentId && <p className="form-error" role="alert">{errors.instrumentId.message}</p>}

        <label>Counterparty ID <input {...register('counterpartyId')} type="number" /></label>
        {errors.counterpartyId && <p className="form-error" role="alert">{errors.counterpartyId.message}</p>}

        <label>Asset class
          <select {...register('assetClass')} defaultValue="">
            <option value="" disabled>Select asset class…</option>
            <option value="EQUITY">EQUITY</option>
            <option value="FIXED_INCOME">FIXED_INCOME</option>
            <option value="FX">FX</option>
            <option value="COMMODITY">COMMODITY</option>
            <option value="DERIVATIVE">DERIVATIVE</option>
          </select>
        </label>
        {errors.assetClass && <p className="form-error" role="alert">{errors.assetClass.message}</p>}

        <label>Side
          <select {...register('side')} defaultValue="">
            <option value="" disabled>Select side…</option>
            <option value="BUY">BUY</option>
            <option value="SELL">SELL</option>
          </select>
        </label>
        {errors.side && <p className="form-error" role="alert">{errors.side.message}</p>}

        <label>Quantity <input {...register('quantity')} type="number" /></label>
        {errors.quantity && <p className="form-error" role="alert">{errors.quantity.message}</p>}

        <label>Price <input {...register('price')} type="number" /></label>
        {errors.price && <p className="form-error" role="alert">{errors.price.message}</p>}

        <label>Trade date <input {...register('tradeDate')} type="date" /></label>
        {errors.tradeDate && <p className="form-error" role="alert">{errors.tradeDate.message}</p>}

        {errors.root && <p className="form-error" role="alert">{errors.root.message}</p>}

        <button disabled={isSubmitting} type="submit">Submit</button>
      </form>
    </section>
  );
}

export default withAuth(AddTrade);
