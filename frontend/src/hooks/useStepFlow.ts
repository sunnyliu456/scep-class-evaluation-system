import { useCallback, useState } from 'react'

export const useStepFlow = (maxStep: number, readyCount: number) => {
  const [step, setStep] = useState(0)
  const [stepReady, setStepReady] = useState<boolean[]>(() =>
    Array.from({ length: readyCount }, () => false)
  )

  const nextStep = useCallback(() => {
    setStep((prev) => (prev < maxStep ? prev + 1 : prev))
  }, [maxStep])

  const prevStep = useCallback(() => {
    setStep((prev) => (prev > 0 ? prev - 1 : prev))
  }, [])

  const markStepReady = useCallback((index: number) => {
    setStepReady((prev) => {
      const next = [...prev]
      next[index] = true
      return next
    })
  }, [])

  return {
    step,
    stepReady,
    nextStep,
    prevStep,
    markStepReady
  }
}
